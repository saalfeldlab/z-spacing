package org.janelia.waves.thickness.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.models.ScaleModel;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;

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
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class EstimateQualityOfSlice {
	
	private final static float[] ONE_DIMENSION_ONE_POSITION = new float[] { 1.0f };

	public static < M extends Model< M >, C extends Model< C > > ArrayImg< DoubleType, DoubleArray > estimate( ArrayImg< DoubleType, 
			DoubleArray> correlations, 
			ArrayImg< DoubleType, DoubleArray > weights, 
			M model,
			ArrayImg< DoubleType, DoubleArray > coordinates,
			RealRandomAccessible< DoubleType > correlationFit, 
			int nThreads ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		ArrayImg<DoubleType, DoubleArray> newWeights = ArrayImgs.doubles( weights.dimension( 0 ) );
		ArrayCursor<DoubleType> newWeightCursor      = newWeights.cursor();
		
		for ( int z = 0; z < correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ); ++z ) {
			
			IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, z, CorrelationsObjectToArrayImg.Z_AXIS ) );
			ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			{
				long currentDZ = -correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) / 2;

				RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
				ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
				
				for ( DoubleType c : correlationsAtBin ) {
					++currentDZ;
					if ( Double.isNaN( c.get() ) ) {
						continue;
					}
					ArrayCursor<DoubleType> weightCursor = weights.cursor();
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
	
	
	public static < M extends Model< M >, C extends Model< C > > ArrayImg< DoubleType, DoubleArray > estimateFromMatrix( ArrayImg< DoubleType, 
			DoubleArray> correlations, 
			ArrayImg< DoubleType, DoubleArray > weights, 
			M model,
			ArrayImg< DoubleType, DoubleArray > coordinates,
			RealRandomAccessible< DoubleType > correlationFit,
			int nThreads ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		ArrayImg<DoubleType, DoubleArray> newWeights         = ArrayImgs.doubles( weights.dimension( 0 ) );
		ArrayCursor<DoubleType> newWeightCursor              = newWeights.cursor();
		RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
		ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
		ArrayRandomAccess<DoubleType> weightRanodmAccess     = weights.randomAccess();
		
		int currentZ;
		
		for ( int z = 0; z < correlations.dimension( 0 ); ++z ) {
			
			IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, 0, z ) );
			ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			currentZ = 0;
			
			coordinateRandomAccess.setPosition( z, 0 );
			double refCoordinate = coordinateRandomAccess.get().get();
			
			for ( DoubleType c : correlationsAtBin ) {
				
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
	
	public static void main(String[] args) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		int nPoints      = 50;
		int nComparisons = 10;
		
		double scale = 0.5;
		
		Random rng = new Random( 100 );
		
		double[] params = new double[] { 0.0, 2.0 };
		
		double[] orig = new double[ nPoints * nPoints ];
		double[] mult = new double[ nPoints * nPoints ];
		double[] fit  = new double[ nPoints ];
		
		double[] weights = new double[ nPoints ];
		ArrayImg<DoubleType, DoubleArray> weightsA = ArrayImgs.doubles( weights, nPoints );
		for ( DoubleType w : weightsA )
			w.set( 1.0 );
		
		double[] coordinates = new double[ nPoints ];
		ArrayImg<DoubleType, DoubleArray> coordinateA = ArrayImgs.doubles( coordinates, nPoints );
		{
			int coord = 0;
			for ( DoubleType c : coordinateA ) {
				c.set( coord );
				++coord;
			}
		}
		
		ArrayImg<DoubleType, DoubleArray> origA = ArrayImgs.doubles( orig, nPoints, nPoints );
		ArrayImg<DoubleType, DoubleArray> multA = ArrayImgs.doubles( mult, nPoints, nPoints );
		ArrayImg<DoubleType, DoubleArray> fitA  = ArrayImgs.doubles( fit, nPoints );
		
		for ( DoubleType o : origA )
			o.set( Double.NaN );
		
		for ( DoubleType m : multA )
			m.set( Double.NaN );
		
		ArrayRandomAccess<DoubleType> origAccess = origA.randomAccess();
		ArrayRandomAccess<DoubleType> multAccess = multA.randomAccess();
		ArrayRandomAccess<DoubleType> fitAccess  = fitA.randomAccess();
		
		for (int i = 0; i < nPoints; ++i) {
			
			origAccess.setPosition( i, 0 );
			multAccess.setPosition( i, 0 );
			fitAccess.setPosition( i, 0 );
			
			fitAccess.get().set( new BellCurve().value( i, params ) );
			
			
			for ( int k = -nComparisons; k <= nComparisons; ++k ) {
				
				if ( i + k < 0 || i + k >= nPoints ) {
					continue;
				}
				
				origAccess.setPosition( i + k, 1 );
				multAccess.setPosition( i + k, 1 );
				
				origAccess.get().set( new BellCurve().value( k, params ) );
				multAccess.get().set( new BellCurve().value( k, params ) * scale * ( 1 + rng.nextGaussian() * 0.1 ) );
				
			}
		}
		
		ArrayImg<DoubleType, DoubleArray> newW = estimateFromMatrix( multA, 
				weightsA, 
				new ScaleModel( 0.0f ), 
				coordinateA, 
				Views.interpolate( Views.extendMirrorSingle( fitA ), new NLinearInterpolatorFactory<DoubleType>() ),
				1);
				
	}
	
}
