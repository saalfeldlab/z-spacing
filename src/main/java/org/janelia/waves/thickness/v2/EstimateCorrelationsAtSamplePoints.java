package org.janelia.waves.thickness.v2;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel1D;

public class EstimateCorrelationsAtSamplePoints {
	
	private final static float[] ONE_DIMENSION_ZERO_POSITION = new float[] { 0.0f };
	public static ArrayImg<DoubleType, DoubleArray> arryImg = ArrayImgs.doubles( 10, 360, 25 );
	public static int t;
	
	public static <M extends Model< M > > ArrayImg< DoubleType, DoubleArray> estimate( ArrayImg< DoubleType, DoubleArray> correlations, ArrayImg< DoubleType, DoubleArray> weights, M model ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) );
		ArrayCursor<DoubleType> resultCursor     = result.cursor();
		
		for ( int i = 0; i < correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ); ++i ) {
			
			Cursor<DoubleType> correlationsCursor = Views.flatIterable( Views.hyperSlice( correlations, CorrelationsObjectToArrayImg.DZ_AXIS, i ) ).cursor();
			ArrayCursor<DoubleType> weightCursor  = weights.cursor();
			
			ArrayList< PointMatch > pointMatches = new ArrayList<PointMatch>();
			
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
	
	public static <M extends Model< M > > ArrayImg< DoubleType, DoubleArray> estimateFromMatrix( RandomAccessibleInterval< DoubleType > correlations, 
			ArrayImg< DoubleType, DoubleArray> weights,
			LUTRealTransform transform,
			int nIter,
			M model,
			double[] variances) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		TreeMap<Integer, ArrayList<PointMatch>> pointCollections = new TreeMap< Integer, ArrayList<PointMatch>>();
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		RealTransformRandomAccessible<DoubleType, LUTRealTransform> source2 = new RealTransformRandomAccessible<DoubleType, LUTRealTransform >( source, transform );
		
		ImageJFunctions.show(Views.interval(Views.offset(source2, -10, -10), new long[]{0,0}, new long[]{correlations.dimension(0) + 20,correlations.dimension(1) + 20}) );
		
		RealRandomAccess<DoubleType> access   = source2.realRandomAccess();
		RealRandomAccess<DoubleType> access2  = source2.realRandomAccess();
		OutOfBounds<DoubleType> weight1 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		OutOfBounds<DoubleType> weight2 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		
		double[] result = new double[ nIter ];
//		result[0] = 1.0;
		
		IntervalView<DoubleType> hyperSlice = Views.hyperSlice( arryImg, 2, t);
		RandomAccess<DoubleType> ra = hyperSlice.randomAccess();
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			
			access2.setPosition(access);
			
			for ( int k = 0; k < nIter; ++k  ) {
				
				
				
				weight1.setPosition( i + k, 0 );
				weight2.setPosition( i - k, 0 );
			
				
				ArrayList<PointMatch> points = pointCollections.get( k );
				if ( points == null ) {
					points = new ArrayList<PointMatch>();
					pointCollections.put( k, points );
				}
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				final double w1 = weight1.get().get();
				final double w2 = weight2.get().get();
				
				ra.setPosition( 2 * i, 1);
				ra.setPosition( k, 0);
				ra.get().set( a1 );
				
				ra.fwd(1);
				ra.get().set( a2 );
				
				if ( ( ! Double.isNaN( a1 ) ) && ( ! Double.isNaN( w1 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a1 } ), (float) w1 ) );
				
				if ( ( ! Double.isNaN( a2 ) ) && ( ! Double.isNaN( w2 ) ) )
					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a2 } ), (float) w2 ) );
				
				access.fwd( 0 );
				access2.bck( 0 );
				
			}
			
		}
		
		// variances[0] = 0.0;
		for ( int i = 0; i < result.length; ++i ) {
			double[] values = new double[ pointCollections.get( i ).size() ];
			for ( int k = 0; k < values.length; ++k ) {
				values[k] = pointCollections.get( i ).get( k ).getP2().getW()[0];
			}
			model.fit( pointCollections.get( i ) );
			result[i] = model.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
			variances[i] = new Variance().evaluate( values );
		}
		
		return ArrayImgs.doubles( result, result.length );
	}
	
	public static void main( String[] args ) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		

	double[] param = new double[] { 0.0, 2.0 };
	
	
		BellCurve func  = new BellCurve();
		
		double[] arr = new double[ 3 * 3 ];
		ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles( arr,  3, 3 );
		
		ArrayCursor<DoubleType> cursor = img.cursor();
		while( cursor.hasNext() ) {
			cursor.fwd();
			double delta = Math.abs( cursor.getDoublePosition( 1 ) - cursor.getDoublePosition( 0 ) );
			if ( delta > 0.1 && ( cursor.getDoublePosition(0) == 2 || cursor.getDoublePosition(1 ) == 2 ) ) {
				delta += 0.5;
			}
			cursor.get().set( func.value( delta, param) );
		}
		
		double[] lut = new double[] { 0, 1.0, 2.6 }; 
		LUTRealTransform lutTransform = new LUTRealTransform( lut, 2, 2 );
		
		ArrayImg<DoubleType, DoubleArray> res = ArrayImgs.doubles( 4, 4 );
		
		RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendBorder(img), new NearestNeighborInterpolatorFactory<DoubleType>() );
		LUTRealTransform.render( source, res, lutTransform, 0.1 );
		
		RealRandomAccessible<DoubleType> view = Views.interpolate( Views.extendBorder( res ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealRandomAccessible<DoubleType> target = new RealTransformRandomAccessible<DoubleType, RealTransform>( source, lutTransform );
		
		for ( int i = 0; i < 3; ++i ) {
			for ( int j = 0; j < 3; ++j ) {
				RandomAccess<DoubleType> ra = Views.interval( Views.raster( view ), new long[] {0, 0}, new long[]{ 3, 3 } ).randomAccess();
				ra.setPosition( new int[] { i, j } );
			}
		}
		
		int nCorrs = 100;
		int nRel   = 7;
		
		double[] correlationMatrix = new double[ nCorrs * nCorrs ];
		for (int i = 0; i < correlationMatrix.length; i++) {
			correlationMatrix[i] = Double.NaN;
		}
		
		ArrayImg<DoubleType, DoubleArray> cImage = ArrayImgs.doubles( correlationMatrix, nCorrs, nCorrs );
		ArrayRandomAccess<DoubleType> cAccess    = cImage.randomAccess();
		
		Random rng = new Random( nCorrs );
		
		double[] params = new double[] { 0.0, 2.0 };
		
		for ( int i = 0; i < nCorrs; ++ i) {
			cAccess.setPosition( i, 0 );
			for ( int j = -nRel; j <= nRel; ++j ) {
				if ( i + j < 0 || i + j >= nCorrs ) {
					continue;
				}
				cAccess.setPosition( i + j, 1 );
				cAccess.get().set( new BellCurve().value( j, params) + rng.nextGaussian() * 0.1 ); //* new BellCurve().value( j, params) );
			}
		}
		
		ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( nCorrs );
		
		for ( DoubleType w : weights ) {
			w.set( 1.0 );
		}
		
//		ArrayImg<DoubleType, DoubleArray> estimate = estimateFromMatrix( cImage, weights, new TranslationModel1D() );
//		
//		System.out.println();
//		
//		int count = 0;
//		
//		for ( DoubleType e : estimate ) {
//			if ( count > nRel ) {
//				break;
//			}
//			System.out.println( e.get() + " (" + new BellCurve().value( count, params) + ")" );
//			++count;
//		}
		
	}
	
	

}
