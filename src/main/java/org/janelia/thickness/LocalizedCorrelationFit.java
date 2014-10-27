package org.janelia.thickness;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.utility.CopyFromIntervalToInterval;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class LocalizedCorrelationFit {
	
	private static final float[] ONE_DIMENSION_ZERO_POSITION = new float[]{ 0.0f };
	
	public <M extends Model< M > > void estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations,
			final double[] coordinates,
			final double[] weights,
			final double[] multipliers,
			final AbstractLUTRealTransform transform,
			final int range,
			final int diameter,
			final M correlationFitModel,
			final ListImg< double[] > localFits) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		assert localFits.numDimensions() == 2;
		assert localFits.dimension( 1 )  == coordinates.length;
		assert localFits.firstElement().length == range;
		
		final long numberOfSections = Math.round( coordinates.length * 1.0 / diameter );
		final double correctedDiameter = coordinates.length * 1.0 / numberOfSections;
		final double halfDiameter = correctedDiameter / 2.0;
		
		final ArrayImg<DoubleType, DoubleArray> extendedFits = ArrayImgs.doubles( numberOfSections + 2, range );
		
		final ArrayList< ArrayList< ArrayList< PointMatch > > > samples = new ArrayList< ArrayList< ArrayList< PointMatch > > >();
		for ( int s = 0; s < numberOfSections; ++s ) {
			final ArrayList<ArrayList<PointMatch>> al = new ArrayList< ArrayList< PointMatch> >();
			for ( int k = 0; k <= range; ++k ) {
				al.add( new ArrayList<PointMatch>() );
			}
			samples.add( al );
		}
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> source2 = RealViews.transformReal(source, transform);
		final LUTRealTransform tf1d = new LUTRealTransform( coordinates, 1, 1);
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> multipliersInterpolatedTransformed = 
				RealViews.transformReal( Views.interpolate( Views.extendValue( ArrayImgs.doubles( multipliers, multipliers.length ), new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>()), 
				tf1d );

		final RealRandomAccess< DoubleType > access  = source2.realRandomAccess();
		final RealRandomAccess< DoubleType > access2 = source2.realRandomAccess();
		
		final RealRandomAccess< DoubleType> m1 = multipliersInterpolatedTransformed.realRandomAccess();
		final RealRandomAccess< DoubleType> m2 = multipliersInterpolatedTransformed.realRandomAccess();
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			access2.setPosition(access);
			
			m1.setPosition( i, 0 );
			tf1d.apply( m1, m1 );
			m2.setPosition( m1 );
			final double mref = m1.get().get();
			
			final int currentSection = (int) (i *1.0 / correlations.dimension( 1 ) * numberOfSections );
			final ArrayList<ArrayList<PointMatch>> pointCollections = samples.get( currentSection );
			
			for ( int k = 0; k <= range; ++k, access.fwd( 0 ), access2.bck( 0 ), m1.fwd( 0 ), m2.bck( 0 ) ) {
				
//				if ( i < coordinates.length - 1 && coordinates[i] + k < coordinates[ i + 1 ] )
//					continue;
//				
//				if ( i > 0 && coordinates[i] - k > coordinates[ i - 1 ] )
//					continue;
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				final ArrayList<PointMatch> pointCollection = pointCollections.get( k );
				
				if ( ! Double.isNaN( a1 ) )
				{
					final int index = i + k;
					if ( index < weights.length ) {
						final float w1 = (float)weights[ index ]; // replace 1.0 by real weight, as soon as weight calculation has become clear
						pointCollection.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)( a1*mref*m1.get().get() ) } ), w1 ) );
					}
				}
				
				if ( ( ! Double.isNaN( a2 ) ) )
				{
					final int index = i - k;
					if ( index > 0 ) {
						final float w2 = (float)weights[ index ]; // replace 1.0 by real weight, as soon as weight calculation has become clear 
						pointCollection.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)( a2*mref*m2.get().get() ) } ), w2 ) );
					}
				}
			}
		}
		
		for ( int s = 0; s < numberOfSections; ++s ) {
			final IntervalView<DoubleType> hs = Views.hyperSlice( extendedFits, 0, s+1 );
			final RandomAccess<DoubleType> ra = hs.randomAccess();
			ra.get().set( -1.0 );
			final ArrayList<ArrayList<PointMatch>> pC = samples.get( s );
			for ( int k = 1; k < range; ++k ) {
				ra.fwd( 0 );
				correlationFitModel.fit( pC.get( k ) );
				/* TODO inverts because LUTRealTransform can only increasing */
				ra.get().set( -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ] );
			}
		}
		
		final IntervalView<DoubleType> zero = Views.hyperSlice( extendedFits, 0, 0 );
		final IntervalView<DoubleType> first = Views.hyperSlice( extendedFits, 0, 1 );
		CopyFromIntervalToInterval.copy( first, zero, new TypeIdentity<DoubleType>());
		
		final IntervalView<DoubleType> last = Views.hyperSlice( extendedFits, 0, extendedFits.dimension(0) - 1 );
		final IntervalView<DoubleType> secondToLast = Views.hyperSlice( extendedFits, 0, extendedFits.dimension(0) - 2 );
		CopyFromIntervalToInterval.copy( secondToLast, last, new TypeIdentity<DoubleType>());
		
		final CompositeIntervalView<DoubleType, RealComposite<DoubleType>> collapsed = Views.collapseReal( extendedFits );
		final RealRandomAccessible<RealComposite<DoubleType>> interpolated = Views.interpolate( collapsed, new NLinearInterpolatorFactory<RealComposite<DoubleType>>());
		
		final ListCursor<double[]> c = localFits.cursor();
		final RealRandomAccess<RealComposite<DoubleType>> i = interpolated.realRandomAccess();
		
		while( c.hasNext() ) {
			c.fwd();
			final double pos = c.getDoublePosition( 0 );
			i.setPosition( pos / correctedDiameter + 0.5, 0 );
			final double[] arr = c.get();
			final RealComposite<DoubleType> composite = i.get();
			for (int j = 0; j < arr.length; j++) {
				arr[j] = composite.get( j ).get();
			}
		}
		
	}
	
	
	public <M extends Model< M > > void estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations,
			final double[] coordinates,
			final double[] weights,
			final double[] multipliers,
			final AbstractLUTRealTransform transform,
			final int range,
			final M correlationFitModel,
			final ListImg< double[] > localFits) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final int diameter = (int) correlations.dimension( 0 );
		this.estimateFromMatrix( correlations, coordinates, weights, multipliers, transform, range, diameter, correlationFitModel, localFits );
	}

}
