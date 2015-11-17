package org.janelia.thickness;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.thickness.cluster.Categorizer;
import org.janelia.thickness.cluster.RangedCategorizer;
import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class LocalizedCorrelationFit {

	private static final double[] ONE_DIMENSION_ZERO_POSITION = new double[]{ 0.0 };

	public static < T extends RealType< T >, M extends Model< M > > void estimateFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] coordinates,
			final AbstractLUTRealTransform transform,
			final int range,
			final int windowRange,
			final M correlationFitModel,
			final ListImg< double[] > localFits,
			final boolean forceMontonicity ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

		final RangedCategorizer categorizer = new RangedCategorizer( windowRange );
		categorizer.generateLabels( windowRange );
		estimateFromMatrix(correlations, coordinates, transform, range, correlationFitModel, categorizer, localFits, forceMontonicity);
	}

	public static < T extends RealType< T >, M extends Model< M > > void estimateFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] coordinates,
			final AbstractLUTRealTransform transform,
			final int range,
			final M correlationFitModel,
			final Categorizer categorizer,
			final ListImg< double[] > localFits,
			final boolean forceMonotonicity) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

		assert localFits.numDimensions() == 2;
		assert localFits.dimension( 1 )  == coordinates.length;

		final double[][] assignments = categorizer.getLabels( coordinates );
		final int numberOfModels = assignments[0].length;

		final ArrayList< ArrayList< ArrayList< PointMatch > > > samples = new ArrayList< ArrayList< ArrayList< PointMatch > > >();
		for ( int s = 0; s < numberOfModels; ++s ) {
			final ArrayList<ArrayList<PointMatch>> al = new ArrayList< ArrayList< PointMatch> >();
			for ( int k = 0; k <= range; ++k ) {
				al.add( new ArrayList<PointMatch>() );
			}
			samples.add( al );
		}

		final T dummy = correlations.randomAccess().get().copy();
		dummy.setReal( Double.NaN );
		final RealRandomAccessible< T > source = Views.interpolate( Views.extendValue( correlations, dummy ), new NLinearInterpolatorFactory< T >() );

		final RealTransformRealRandomAccessible< T, InverseRealTransform> source2 = RealViews.transformReal(source, transform);

		final RealRandomAccess< T > access1 = source2.realRandomAccess();
		final RealRandomAccess< T > access2 = source2.realRandomAccess();

		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {

			access1.setPosition( i, 1 );
			access1.setPosition( i, 0 );

			transform.apply(access1, access1);
			access2.setPosition(access1);

			final double[] currentAssignment = assignments[i];

			double currentMin1 = Double.MAX_VALUE;
			double currentMin2 = Double.MAX_VALUE;

			for ( int k = 0; k <= range; ++k, access1.fwd( 0 ), access2.bck( 0 ) ) {

				final double a1 = access1.get().getRealDouble();
				final double a2 = access2.get().getRealDouble();

				if ( ( ! Double.isNaN( a1 ) ) && ( a1 > 0.0 ) && !( forceMonotonicity && a1 > currentMin1 ) )
				{
					currentMin1 = a1;
					for ( int modelIndex = 0; modelIndex < currentAssignment.length; ++modelIndex )
						samples.get( modelIndex ).get( k ).add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new double[]{ a1 } ) ) );
				}

				if ( ( ! Double.isNaN( a2 ) ) && ( a2 > 0.0 ) && !( forceMonotonicity && a2 > currentMin2 ) )
				{
					final int index = i - k;
					currentMin2 = a2;
						for ( int modelIndex = 0; modelIndex < currentAssignment.length; ++modelIndex )
							samples.get( modelIndex ).get( k ).add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new double[]{ a2 } ) ) );
				}
			}
		}

		final ArrayImg<DoubleType, DoubleArray> fits = ArrayImgs.doubles( numberOfModels, range );
		for ( int m = 0; m < numberOfModels; ++m ) {
			final Cursor<DoubleType> cursor = Views.flatIterable( Views.hyperSlice( fits, 0, m ) ).cursor();
			final ArrayList<ArrayList<PointMatch>> points = samples.get( m );
			/* TODO inverts because LUTRealTransform can only increasing */
			cursor.next().set( -1.0 );
			for ( int k = 1; cursor.hasNext(); ++k ) {
				correlationFitModel.fit( points.get( k ) );
				/* TODO inverts because LUTRealTransform can only increasing */
				cursor.next().set( -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[0] );
			}
		}

		final ListCursor<double[]> c = localFits.cursor();
		while( c.hasNext() ) {
			c.fwd();
			final double[] arr = c.get();
			for (int j = 0; j < arr.length; j++) {
				arr[j] = 0.0;
			}
			final int pos = c.getIntPosition( 0 );
			final double[] localAssignments = assignments[pos];
			for ( int m = 0; m < numberOfModels; ++m ) {
				final Cursor<DoubleType> cursor = Views.flatIterable( Views.hyperSlice( fits, 0, m ) ).cursor();
				for ( int k = 0; cursor.hasNext(); ++k ) {
					arr[k] += cursor.next().get() * localAssignments[m];
				}
			}
			double val = 0.5 * (3.0 * arr[1] - arr[2]);
			double reciprocal = -1.0 / val;
			arr[0] = val;
			for( int i = 0; i < arr.length; ++i )
			{
				arr[i] *= reciprocal;
			}
		}

	}

	public static < T extends RealType< T >, M extends Model< M > > void estimateFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] coordinates,
			final AbstractLUTRealTransform transform,
			final int range,
			final M correlationFitModel,
			final ListImg< double[] > localFits,
			final boolean forceMonotonicity ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final int windowRange = coordinates.length;
		estimateFromMatrix( correlations, coordinates, transform, range, windowRange, correlationFitModel, localFits, forceMonotonicity );
	}


}
