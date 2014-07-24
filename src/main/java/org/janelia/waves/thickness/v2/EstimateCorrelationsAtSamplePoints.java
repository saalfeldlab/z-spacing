package org.janelia.waves.thickness.v2;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.stat.descriptive.moment.Variance;

public class EstimateCorrelationsAtSamplePoints {
	
	private final static float[] ONE_DIMENSION_ZERO_POSITION = new float[] { 0.0f };
	
	public static <M extends Model< M > > ArrayImg< DoubleType, DoubleArray> estimate( final ArrayImg< DoubleType, DoubleArray> correlations, final ArrayImg< DoubleType, DoubleArray> weights, final M model ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) );
		final ArrayCursor<DoubleType> resultCursor     = result.cursor();
		
		for ( int i = 0; i < correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ); ++i ) {
			
			final Cursor<DoubleType> correlationsCursor = Views.flatIterable( Views.hyperSlice( correlations, CorrelationsObjectToArrayImg.DZ_AXIS, i ) ).cursor();
			final ArrayCursor<DoubleType> weightCursor  = weights.cursor();
			
			final ArrayList< PointMatch > pointMatches = new ArrayList<PointMatch>();
			
			while ( correlationsCursor.hasNext() ) {
				correlationsCursor.fwd();
				if ( Double.isNaN( correlationsCursor.get().get() ) ) {
					continue;
				}
				pointMatches.add( new PointMatch( new Point( new float[] { 0.0f } ), new Point( new float[] { correlationsCursor.get().getRealFloat() }), weightCursor.next().getRealFloat() ) );
			}
			
			model.fit( pointMatches );
			
			resultCursor.next().set( model.apply( ONE_DIMENSION_ZERO_POSITION )[0] );
			
		}
		
		return result;
		
	}
	
	public static <M extends Model< M > > ArrayImg< DoubleType, DoubleArray> estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations, 
			final ArrayImg< DoubleType, DoubleArray> weights,
			final LUTRealTransform transform,
			final double[] coordinates,
			final int nIter,
			final M model,
			final double[] variances) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final double[] accumulativeShifts = new double[ coordinates.length ];
		
		accumulativeShifts[0] = coordinates[0];
		for (int i = 1; i < accumulativeShifts.length; i++) {
			accumulativeShifts[ i ] = accumulativeShifts[ i - 1 ] + coordinates[ i ] - i;
		}
		
		final TreeMap<Integer, ArrayList<PointMatch>> pointCollections = new TreeMap< Integer, ArrayList<PointMatch>>();
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> source2 = RealViews.transformReal(source, transform);
		
		
		final RealRandomAccess<DoubleType> access   = source2.realRandomAccess();
		final RealRandomAccess<DoubleType> access2  = source2.realRandomAccess();
		
		
		final OutOfBounds<DoubleType> weight1 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		final OutOfBounds<DoubleType> weight2 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		
		final double[] result = new double[ nIter ];
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			access2.setPosition(access);
			
			for ( int k = 0; k <= nIter; ++k ) {
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				final double w1 = 1.0; // weight1.get().get(); // replace 1.0 by real weight, as soon as weight calculation has become clear 
				final double w2 = 1.0; // weight2.get().get(); // replace 1.0 by real weight, as soon as weight calculation has become clear 
				
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
			final double[] values = new double[ pointCollections.get( i ).size() ];
			for ( int k = 0; k < values.length; ++k ) {
				values[k] = pointCollections.get( i ).get( k ).getP2().getW()[0];
			}
			model.fit( pointCollections.get( i ) );
			result[i] = model.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
			variances[i] = new Variance().evaluate( values );
		}
		
		return ArrayImgs.doubles( result, result.length );
	}
	
	public static void main( final String[] args ) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		

//	final double[] param = new double[] { 0.0, 2.0 };
//	
//	
//		final BellCurve func  = new BellCurve();
//		
//		final double[] arr = new double[ 3 * 3 ];
//		final ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles( arr,  3, 3 );
//		
//		final ArrayCursor<DoubleType> cursor = img.cursor();
//		while( cursor.hasNext() ) {
//			cursor.fwd();
//			double delta = Math.abs( cursor.getDoublePosition( 1 ) - cursor.getDoublePosition( 0 ) );
//			if ( delta > 0.1 && ( cursor.getDoublePosition(0) == 2 || cursor.getDoublePosition(1 ) == 2 ) ) {
//				delta += 0.5;
//			}
//			cursor.get().set( func.value( delta, param) );
//		}
//		
//		final double[] lut = new double[] { 0, 1.0, 2.6 }; 
//		final LUTRealTransform lutTransform = new LUTRealTransform( lut, 2, 2 );
//		
//		final ArrayImg<DoubleType, DoubleArray> res = ArrayImgs.doubles( 4, 4 );
//		
//		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendBorder(img), new NearestNeighborInterpolatorFactory<DoubleType>() );
//		LUTRealTransform.render( source, res, lutTransform, 0.1 );
//		
//		final RealRandomAccessible<DoubleType> view = Views.interpolate( Views.extendBorder( res ), new NLinearInterpolatorFactory<DoubleType>());
//		
//		final RealRandomAccessible<DoubleType> target = new RealTransformRandomAccessible<DoubleType, RealTransform>( source, lutTransform );
//		
//		for ( int i = 0; i < 3; ++i ) {
//			for ( int j = 0; j < 3; ++j ) {
//				final RandomAccess<DoubleType> ra = Views.interval( Views.raster( view ), new long[] {0, 0}, new long[]{ 3, 3 } ).randomAccess();
//				ra.setPosition( new int[] { i, j } );
//			}
//		}
//		
//		final int nCorrs = 100;
//		final int nRel   = 7;
//		
//		final double[] correlationMatrix = new double[ nCorrs * nCorrs ];
//		for (int i = 0; i < correlationMatrix.length; i++) {
//			correlationMatrix[i] = Double.NaN;
//		}
//		
//		final ArrayImg<DoubleType, DoubleArray> cImage = ArrayImgs.doubles( correlationMatrix, nCorrs, nCorrs );
//		final ArrayRandomAccess<DoubleType> cAccess    = cImage.randomAccess();
//		
//		final Random rng = new Random( nCorrs );
//		
//		final double[] params = new double[] { 0.0, 2.0 };
//		
//		for ( int i = 0; i < nCorrs; ++ i) {
//			cAccess.setPosition( i, 0 );
//			for ( int j = -nRel; j <= nRel; ++j ) {
//				if ( i + j < 0 || i + j >= nCorrs ) {
//					continue;
//				}
//				cAccess.setPosition( i + j, 1 );
//				cAccess.get().set( new BellCurve().value( j, params) + rng.nextGaussian() * 0.1 ); //* new BellCurve().value( j, params) );
//			}
//		}
//		
//		final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( nCorrs );
//		
//		for ( final DoubleType w : weights ) {
//			w.set( 1.0 );
//		}
		
	}
	
	

}
