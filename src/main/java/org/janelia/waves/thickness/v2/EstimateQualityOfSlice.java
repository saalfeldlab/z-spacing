package org.janelia.waves.thickness.v2;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.IterableInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;

public class EstimateQualityOfSlice {
	
	private final static float[] ONE_DIMENSION_ONE_POSITION = new float[] { 1.0f };

	public static < M extends Model< M >, C extends Model< C > > ArrayImg< DoubleType, DoubleArray > estimate( final ArrayImg< DoubleType, 
			DoubleArray> correlations, 
			final ArrayImg< DoubleType, DoubleArray > weights, 
			final M model,
			final ArrayImg< DoubleType, DoubleArray > coordinates,
			final RealRandomAccessible< DoubleType > correlationFit, 
			final int nThreads ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final ArrayImg<DoubleType, DoubleArray> newWeights = ArrayImgs.doubles( weights.dimension( 0 ) );
		final ArrayCursor<DoubleType> newWeightCursor      = newWeights.cursor();
		
		for ( int z = 0; z < correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ); ++z ) {
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, z, CorrelationsObjectToArrayImg.Z_AXIS ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			{
				long currentDZ = -correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) / 2;

				final RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
				final ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
				
				for ( final DoubleType c : correlationsAtBin ) {
					++currentDZ;
					if ( Double.isNaN( c.get() ) ) {
						continue;
					}
					final ArrayCursor<DoubleType> weightCursor = weights.cursor();
					weightCursor.jumpFwd( z + currentDZ );
					coordinateRandomAccess.setPosition( new long[] { z + currentDZ } );
					fitRandomAccess.setPosition( new double[] { coordinateRandomAccess.get().get() } );
					pointMatches.add( new PointMatch( new Point( new float[] { c.getRealFloat() } ), new Point( new float[] { fitRandomAccess.get().getRealFloat() } ), weightCursor.get().getRealFloat() ) );
				}
			}
			
			model.fit( pointMatches );
			
			newWeightCursor.next().set( model.apply( ONE_DIMENSION_ONE_POSITION )[0] );
			
		}
	
		return newWeights;
	}
	
	
	public static < M extends Model< M >, C extends Model< C > > ArrayImg< DoubleType, DoubleArray > estimateFromMatrix( final ArrayImg< DoubleType, 
			DoubleArray> correlations, 
			final ArrayImg< DoubleType, DoubleArray > weights, 
			final M model,
			final ArrayImg< DoubleType, DoubleArray > coordinates,
			final RealRandomAccessible< DoubleType > correlationFit,
			final int nThreads ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final ArrayImg<DoubleType, DoubleArray> newWeights         = ArrayImgs.doubles( weights.dimension( 0 ) );
		final ArrayCursor<DoubleType> newWeightCursor              = newWeights.cursor();
		final RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
		final ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
		final ArrayRandomAccess<DoubleType> weightRanodmAccess     = weights.randomAccess();
		
		int currentZ;
		
		for ( int z = 0; z < correlations.dimension( 0 ); ++z ) {
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, 0, z ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			currentZ = 0;
			
			coordinateRandomAccess.setPosition( z, 0 );
			final double refCoordinate = coordinateRandomAccess.get().get();
			
			for ( final DoubleType c : correlationsAtBin ) {
				
				coordinateRandomAccess.setPosition( currentZ, 0);
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get() - refCoordinate, 0 );
				weightRanodmAccess.setPosition( currentZ, 0 );
				
				++currentZ;
				if ( Float.isNaN( c.getRealFloat() ) || Float.isNaN(fitRandomAccess.get().getRealFloat() ) ) {
					continue;
				}
				
				pointMatches.add( new PointMatch( new Point( new float[] { c.getRealFloat() } ), new Point( new float[] { fitRandomAccess.get().getRealFloat() }), weightRanodmAccess.get().getRealFloat() ) );
				
				
				
				
				
			}
	
			model.fit( pointMatches );
			
			newWeightCursor.next().set( model.apply( ONE_DIMENSION_ONE_POSITION )[0] );
			
		}
		return newWeights;
	}
	
	public static void main(final String[] args) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		
//		final int nPoints      = 50;
//		final int nComparisons = 10;
//		
//		final double scale = 0.5;
//		
//		final Random rng = new Random( 100 );
//		
//		final double[] params = new double[] { 0.0, 2.0 };
//		
//		final double[] orig = new double[ nPoints * nPoints ];
//		final double[] mult = new double[ nPoints * nPoints ];
//		final double[] fit  = new double[ nPoints ];
//		
//		final double[] weights = new double[ nPoints ];
//		final ArrayImg<DoubleType, DoubleArray> weightsA = ArrayImgs.doubles( weights, nPoints );
//		for ( final DoubleType w : weightsA )
//			w.set( 1.0 );
//		
//		final double[] coordinates = new double[ nPoints ];
//		final ArrayImg<DoubleType, DoubleArray> coordinateA = ArrayImgs.doubles( coordinates, nPoints );
//		{
//			int coord = 0;
//			for ( final DoubleType c : coordinateA ) {
//				c.set( coord );
//				++coord;
//			}
//		}
//		
//		final ArrayImg<DoubleType, DoubleArray> origA = ArrayImgs.doubles( orig, nPoints, nPoints );
//		final ArrayImg<DoubleType, DoubleArray> multA = ArrayImgs.doubles( mult, nPoints, nPoints );
//		final ArrayImg<DoubleType, DoubleArray> fitA  = ArrayImgs.doubles( fit, nPoints );
//		
//		for ( final DoubleType o : origA )
//			o.set( Double.NaN );
//		
//		for ( final DoubleType m : multA )
//			m.set( Double.NaN );
//		
//		final ArrayRandomAccess<DoubleType> origAccess = origA.randomAccess();
//		final ArrayRandomAccess<DoubleType> multAccess = multA.randomAccess();
//		final ArrayRandomAccess<DoubleType> fitAccess  = fitA.randomAccess();
//		
//		for (int i = 0; i < nPoints; ++i) {
//			
//			origAccess.setPosition( i, 0 );
//			multAccess.setPosition( i, 0 );
//			fitAccess.setPosition( i, 0 );
//			
//			fitAccess.get().set( new BellCurve().value( i, params ) );
//			
//			
//			for ( int k = -nComparisons; k <= nComparisons; ++k ) {
//				
//				if ( i + k < 0 || i + k >= nPoints ) {
//					continue;
//				}
//				
//				origAccess.setPosition( i + k, 1 );
//				multAccess.setPosition( i + k, 1 );
//				
//				origAccess.get().set( new BellCurve().value( k, params ) );
//				multAccess.get().set( new BellCurve().value( k, params ) * scale * ( 1 + rng.nextGaussian() * 0.1 ) );
//				
//			}
//		}
//		
//		final ArrayImg<DoubleType, DoubleArray> newW = estimateFromMatrix( multA, 
//				weightsA, 
//				new ScaleModel( 0.0f ), 
//				coordinateA, 
//				Views.interpolate( Views.extendMirrorSingle( fitA ), new NLinearInterpolatorFactory<DoubleType>() ),
//				1);
//				
	}
	
}
