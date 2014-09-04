package org.janelia.thickness;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTGrid;

public class EstimateCorrelationsAtSamplePoints {
	
	private final static float[] ONE_DIMENSION_ZERO_POSITION = new float[] { 0.0f };
	
	public static <M extends Model< M > > double[] estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations, 
			final double[] weights,
			final AbstractLUTRealTransform transform,
			final double[] coordinates,
			final int nIter,
			final M correlationFitModel,
			final double[] variances) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final double[] accumulativeShifts = new double[ coordinates.length ];
		
		accumulativeShifts[0] = coordinates[0];
		for (int i = 1; i < accumulativeShifts.length; i++) {
			accumulativeShifts[ i ] = accumulativeShifts[ i - 1 ] + coordinates[ i ] - i;
		}
		
		
		
		final TreeMap<Integer, ArrayList<PointMatch>> pointCollections = new TreeMap< Integer, ArrayList<PointMatch>>();
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> source2 = RealViews.transformReal(source, transform);

		final RealRandomAccess< DoubleType > access  = source2.realRandomAccess();
		final RealRandomAccess< DoubleType > access2 = source2.realRandomAccess();
		
		
		final double[] result = new double[ nIter ];
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			access2.setPosition(access);
			
			for ( int k = 0; k <= nIter; ++k ) {
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				final double w1 = 1.0; // weights[i + k]; // replace 1.0 by real weight, as soon as weight calculation has become clear 
				final double w2 = 1.0; // weights[i - k]; // replace 1.0 by real weight, as soon as weight calculation has become clear 
				
				ArrayList<PointMatch> points = pointCollections.get( k );
				if ( points == null ) {
					points = new ArrayList<PointMatch>();
					pointCollections.put( k, points );
				}
				
				if ( ( ! Double.isNaN( a1 ) ) && ( ! Double.isNaN( w1 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a1 } ), (float) w1 ) );
				
				if ( ( ! Double.isNaN( a2 ) ) && ( ! Double.isNaN( w2 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a2 } ), (float) w2 ) );
				
				access.fwd(0);
				access2.bck(0);
				
			}
					
		}
		
		for ( int i = 0; i < result.length; ++i ) {
			correlationFitModel.fit( pointCollections.get( i ) );
			
			/* TODO inverts because LUTRealTransform can only increasing */
			result[i] = -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
		}
		
		return result;
	}
	
	
	public static <M extends Model< M > > double[] estimateFromMatrix( 
			final ArrayImg<DoubleType, DoubleArray> matrices,
			final RandomAccessibleInterval< DoubleType > localWeights,
			final LUTGrid lutGrid, 
			final RandomAccessibleInterval< DoubleType > localCoordinates,
			final int comparisonRange,
			final M correlationFitModel,
			final int x,
			final int y ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
final TreeMap<Integer, ArrayList<PointMatch>> pointCollections = new TreeMap< Integer, ArrayList<PointMatch>>();
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( matrices, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> source2 = RealViews.transformReal( source, lutGrid );
		
		final RealRandomAccess<DoubleType> access1 = source2.realRandomAccess();
		final RealRandomAccess<DoubleType> access2 = source2.realRandomAccess();
		
		final RandomAccess<DoubleType> wAccess1 = Views.extendValue( localWeights, new DoubleType(Double.NaN ) ).randomAccess();
		final RandomAccess<DoubleType> wAccess2 = Views.extendValue( localWeights, new DoubleType(Double.NaN ) ).randomAccess();
		
		final int diminishedComparisonRange = comparisonRange - 1;
		final double[] result = new double[ comparisonRange ];
		
		access1.setPosition( x, 0 );
		access1.setPosition( y, 1 );
		
		final int XX = 203;
		
//		if ( x == XX && y == 0 ) {
//		
//			IJ.log( x + " " + matrices.numDimensions() + " vs " + 
//					source2.numDimensions() + 
//					" sourceDim=" + lutGrid.numSourceDimensions() + 
//					" targetDim=" + lutGrid.numTargetDimensions() );
//		
//			final IntervalView<DoubleType> v1 = Views.hyperSlice( Views.hyperSlice(matrices, 1, y), 0, x );
//			final IntervalView<DoubleType> v2 = Views.interval( Views.hyperSlice( Views.hyperSlice( Views.raster( source2 ), 1, y), 0, x), new FinalInterval( v1 ) );
//			ImageJFunctions.show( v1 );
//			ImageJFunctions.show( v2 );
//			IJ.log( String.format( "v1.dimension=(%d,%d)", v1.dimension( 0 ), v1.dimension( 1 ) ) );
//			IJ.log( String.format( "v2.dimension=(%d,%d)", v2.dimension( 0 ), v2.dimension( 1 ) ) );
//			final double[] a = new double[0];
////			a[1] = 123;
//		}
		
		for ( int i = 0; i < matrices.dimension( 3 ); ++i ) {
			
			access1.setPosition( i, 3 );
			access1.setPosition( i, 2 );
		
			wAccess1.setPosition( i, 0 );
			wAccess2.setPosition( i, 0 );
			
			lutGrid.apply(access1, access1);
			access2.setPosition(access1);
			
			for ( int k = 0; k <= comparisonRange; ++k ) {
				
				final double a1 = access1.get().get();
				final double a2 = access2.get().get();
				
				final double w1 = 1.0; // wAccess1.get().get();
				final double w2 = 1.0; // wAccess2.get().get();
				
				ArrayList<PointMatch> points = pointCollections.get( k );
				if ( points == null ) {
					points = new ArrayList<PointMatch>();
					pointCollections.put( k, points );
				}
				
				if ( ( ! Double.isNaN( a1 ) ) && ( ! Double.isNaN( w1 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a1 } ), (float) w1 ) );
				
				if ( ( ! Double.isNaN( a2 ) ) && ( ! Double.isNaN( w2 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a2 } ), (float) w2 ) );
				
				access1.fwd( 2 );
				access2.bck( 2 );
				
				wAccess1.fwd( 0 );
				wAccess2.bck( 0 );
				
			}
		}
		
		for ( int i = 0; i < result.length; ++i ) {
			correlationFitModel.fit( pointCollections.get( i ) );
			
			/* TODO inverts because LUTRealTransform can only increasing */
			result[i] = -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
		}
		
		
		return result;
	}
	
	
	public static <M extends Model< M > > double[] estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations, 
			final double[] weights,
			final AbstractLUTRealTransform transform,
			final double[] coordinates,
			final int nIter,
			final M correlationFitModel ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		return estimateFromMatrix(correlations, weights, transform, coordinates, nIter, correlationFitModel, new double[0]);
	}

}
