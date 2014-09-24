package org.janelia.thickness;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.utilityarrays.MirrorAndExtend;

public class EstimateQualityOfSlice {
	
	private final static float[] ONE_DIMENSION_ONE_POSITION = new float[] { 1.0f };

	public static < M extends Model< M >, C extends Model< C > > double[] estimateFromMatrix(
			final ArrayImg< DoubleType, DoubleArray > correlations,
			final double[] weights,
			final M model,
			final ArrayImg< DoubleType, DoubleArray > coordinates,
			final RealRandomAccessible< DoubleType > correlationFit,
			final int nThreads,
			final double regularizerWeight ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final double[] multipliers = new double[ weights.length ];
		final RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
		final ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
		
		final double inverseRegularizerWeight = 1 - regularizerWeight;
		
		for ( int z = 0; z < correlations.dimension( 0 ); ++z ) {
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, 0, z ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			coordinateRandomAccess.setPosition( z, 0 );
			final double refCoordinate = coordinateRandomAccess.get().get();
			
			final Cursor< DoubleType > correlationBinCursor = correlationsAtBin.localizingCursor();
			while ( correlationBinCursor.hasNext() ) {
				
				final DoubleType tc = correlationBinCursor.next();
				final int currentZ = correlationBinCursor.getIntPosition( 0 );
				
				if ( currentZ == z )
					continue;
				
				final double c = tc.get();
				coordinateRandomAccess.setPosition( currentZ, 0);
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get() - refCoordinate, 0 );

				final double fra = fitRandomAccess.get().get();
				
				if ( Double.isNaN( c ) || Double.isNaN( fra ) )
					continue;
				
				/* TODO inverts because LUTRealTransform can only increasing */
				pointMatches.add(
						new PointMatch(
								new Point( new float[]{ ( float )c } ),
								new Point( new float[]{ -( float )fra } ),
								(float)weights[ currentZ ] ) );
				
			}
	
			model.fit( pointMatches );
			
			/* set factor regularized towards 1.0 */
			multipliers[ z ] = model.apply( ONE_DIMENSION_ONE_POSITION )[0] * inverseRegularizerWeight + regularizerWeight;
			
		}
		return multipliers;
	}
	
	
	public static < M extends Model< M >, C extends Model< C > > double[] estimateFromMatrix(
			final ArrayImg< DoubleType, DoubleArray > correlations,
			final double[] weights,
			final M model,
			final ArrayImg< DoubleType, DoubleArray > coordinates,
			final ListImg< double[] > localFits,
			final int nThreads,
			final double regularizerWeight ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final double[] multipliers = new double[ weights.length ];
		final ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
		
		final double inverseRegularizerWeight = 1 - regularizerWeight;
		
		final ListCursor<double[]> cursor = localFits.cursor();
		
		for ( int z = 0; cursor.hasNext(); ++z ) {
			
			cursor.fwd();
			final RealRandomAccessible<DoubleType> correlationFit = MirrorAndExtend.doubles( cursor.get(), new NLinearInterpolatorFactory< DoubleType >() );
			final RealRandomAccess<DoubleType> fitRandomAccess = correlationFit.realRandomAccess();
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, 0, z ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			coordinateRandomAccess.setPosition( z, 0 );
			final double refCoordinate = coordinateRandomAccess.get().get();
			
			final Cursor< DoubleType > correlationBinCursor = correlationsAtBin.localizingCursor();
			while ( correlationBinCursor.hasNext() ) {
				
				final DoubleType tc = correlationBinCursor.next();
				final int currentZ = correlationBinCursor.getIntPosition( 0 );
				
				if ( currentZ == z )
					continue;
				
				final double c = tc.get();
				coordinateRandomAccess.setPosition( currentZ, 0);
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get() - refCoordinate, 0 );

				final double fra = fitRandomAccess.get().get();
				
				if ( Double.isNaN( c ) || Double.isNaN( fra ) )
					continue;
				
				/* TODO inverts because LUTRealTransform can only increasing */
				pointMatches.add(
						new PointMatch(
								new Point( new float[]{ ( float )c } ),
								new Point( new float[]{ -( float )fra } ),
								(float)weights[ currentZ ] ) );
				
			}
	
			model.fit( pointMatches );
			
			/* set factor regularized towards 1.0 */
			multipliers[ z ] = model.apply( ONE_DIMENSION_ONE_POSITION )[0] * inverseRegularizerWeight + regularizerWeight;
			
		}
		return multipliers;
	}
	
	public static < M extends Model< M > > double[] estimateFromMatrix(
			final RandomAccessibleInterval< DoubleType > localMatrix,
			final RandomAccessibleInterval< DoubleType > localWeights,
			final M model,
			final RandomAccessibleInterval< DoubleType > localCoordinates,
			final RealRandomAccessible< DoubleType > correlationFit,
			final double regularizerWeight
			) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final double[] multipliers = new double[ (int) localWeights.dimension( 0 ) ];
		final RealRandomAccess<DoubleType> fitRandomAccess    = correlationFit.realRandomAccess();
		final RandomAccess<DoubleType> coordinateRandomAccess = localCoordinates.randomAccess();
		
		final double inverseRegularizerWeight = 1 - regularizerWeight;
		
		final RandomAccess<DoubleType> weightAccess = localWeights.randomAccess();
		
		
		for ( int z = 0; z < localMatrix.dimension( 0 ); ++z ) {
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( localMatrix, 0, z ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			coordinateRandomAccess.setPosition( z, 0 );
			final double refCoordinate = coordinateRandomAccess.get().get();
			
			final Cursor< DoubleType > correlationBinCursor = correlationsAtBin.localizingCursor();
			while ( correlationBinCursor.hasNext() ) {
				
				final DoubleType tc = correlationBinCursor.next();
				final int currentZ = correlationBinCursor.getIntPosition( 0 );
				
				if ( currentZ == z )
					continue;
				
				weightAccess.setPosition( z, 0 );
				
				final double c = tc.get();
				coordinateRandomAccess.setPosition( currentZ, 0);
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get() - refCoordinate, 0 );

				final double fra = fitRandomAccess.get().get();
				
				if ( Double.isNaN( c ) || Double.isNaN( fra ) )
					continue;
				
				/* TODO inverts because LUTRealTransform can only increasing */
				pointMatches.add(
						new PointMatch(
								new Point( new float[]{ ( float )c } ),
								new Point( new float[]{ -( float )fra } ),
								weightAccess.get().getRealFloat()) );
				
			}
	
			model.fit( pointMatches );
			
			/* set factor regularized towards 1.0 */
			multipliers[ z ] = model.apply( ONE_DIMENSION_ONE_POSITION )[0] * inverseRegularizerWeight + regularizerWeight;
			
		}
		
		
		return multipliers;
	}
	
}
