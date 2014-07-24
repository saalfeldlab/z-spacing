package org.janelia.waves.thickness.v2;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

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
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;

public class EstimateCorrelationsAtSamplePoints {
	
	private final static float[] ONE_DIMENSION_ZERO_POSITION = new float[] { 0.0f };
	public static ArrayImg<DoubleType, DoubleArray> arryImg = ArrayImgs.doubles( 20, 180, 25 );
	public static ArrayImg<DoubleType, DoubleArray> matrixImg = ArrayImgs.doubles( 1, 1, 1 );
	public static int t;
	
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
		
//		RealTransformRandomAccessible<DoubleType, LUTRealTransform> source2 = new RealTransformRandomAccessible<DoubleType, LUTRealTransform >( source, transform );
		
		// vis with imagej
////		final RandomAccessibleOnRealRandomAccessible<DoubleType> raster = Views.raster( source2 );
//		final Scale2D scale = new Scale2D( 20, 20 );
//		final IntervalView<DoubleType> window = Views.hyperSlice( matrixImg, 2, t );
//		final Cursor<DoubleType> windowCursor = Views.flatIterable(window).cursor();
//		final RealRandomAccessible<DoubleType> sourceInterpolatedNN = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NearestNeighborInterpolatorFactory<DoubleType>());
//		final AffineRealRandomAccessible<DoubleType, AffineGet> sourceNN = RealViews.affineReal( RealViews.transformReal(sourceInterpolatedNN, transform), scale);
////		final Cursor<DoubleType> matrixCursor = Views.flatIterable( Views.interval( Views.raster( sourceNN ), new FinalInterval( correlations.dimension(0) * 5, correlations.dimension( 1 ) * 5 ) ) ).cursor();
////		final Cursor<DoubleType> matrixCursor = Views.flatIterable( Views.interval( Views.raster( sourceNN ), new FinalInterval( new long[]{ 1000, 1000 }, new long[]{ 1511, 1511 } ) ) ).cursor();
//		final Cursor<DoubleType> matrixCursor = Views.flatIterable( Views.interval( Views.raster( sourceNN ), new FinalInterval( new long[]{ 780, 780 }, new long[]{ 780 + 511, 780 + 511 } ) ) ).cursor();
//		while ( matrixCursor.hasNext() ) {
//			windowCursor.next().set( matrixCursor.next() );
//		}
//		ImageJFunctions.show( Views.interval( Views.raster( source2 ), new FinalInterval( correlations.dimension(0), correlations.dimension( 1 ) ) ) );
//		ImageJFunctions.show(Views.interval( raster, new FinalInterval( correlations.dimension( 0 ) + 20, correlations.dimension( 1 ) + 20)));
		// end vis
		
		final RealRandomAccess<DoubleType> access   = source2.realRandomAccess();
		final RealRandomAccess<DoubleType> access2  = source2.realRandomAccess();
		
		
//		RealRandomAccess<DoubleType> access2  = source.realRandomAccess();
		final OutOfBounds<DoubleType> weight1 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		final OutOfBounds<DoubleType> weight2 = Views.extendValue( weights, new DoubleType( Double.NaN ) ).randomAccess();
		
		final double[] result = new double[ nIter ];
//		result[0] = 1.0;
		
		// visualization with imagej
//		final IntervalView<DoubleType> hyperSlice = Views.hyperSlice( arryImg, 2, t);
//		final RandomAccess<DoubleType> ra = hyperSlice.randomAccess();
		// end vis
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			access2.setPosition(access);
			
			for ( int k = 0; k <= nIter; ++k ) {
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				final double w1 = 1.0; // weight1.get().get();
				final double w2 = 1.0; // weight2.get().get();
				
				// visualization with imagej
//				ra.setPosition( 2*i, 1);
//				ra.setPosition( k, 0);
//				ra.get().set( a1 );
//				
//				ra.fwd(1);
//				ra.get().set( a2 );
				// end vis
				
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
			
			
			
//			DELETE IF ABOVE IS SUCCESSFUL
			
//			for ( int k = -nIter; k <= nIter; ++k  ) {
//				
//				if ( i + k < 0 || k + i >= coordinates.length ) {
//					continue;
//				}
//				
//				final double dz = coordinates[i+k] - zRef;
//				
//				
//				// move starting on grid, but non-integer step
//				final double pos = i + dz;
//				
//				
//				access.setPosition( i + k, 0 );
//				access2.setPosition( i + k, 0 );
//				
//				weight1.setPosition( i + k, 0 );
////				weight2.setPosition( i - k, 0 );
//			
//				
//				// assume symmetry -> Math.abs 
//				ArrayList<PointMatch> points = pointCollections.get( Math.abs( k ) );
//				if ( points == null ) {
//					points = new ArrayList<PointMatch>();
//					pointCollections.put( k, points );
//				}
//				
//				final double a1 = access2.get().get();
////				final double a2 = access2.get().get();
//				
//				final double w1 = 1.0; // weight1.get().get();
////				final double w2 = 1.0; // weight2.get().get();
//				
//				// visualization with imagej
//				ra.setPosition( i, 1);
//				ra.setPosition( k + nIter, 0);
//				ra.get().set( a1 );
//				
////				ra.fwd(1);
////				ra.get().set( a2 );
//				// end vis
//				
//				if ( ( ! Double.isNaN( a1 ) ) && ( ! Double.isNaN( w1 ) ) )
//					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a1 } ), (float) w1 ) );
//				
////				if ( ( ! Double.isNaN( a2 ) ) && ( ! Double.isNaN( w2 ) ) )
////					points.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a2 } ), (float) w2 ) );
//				
////				access.fwd( 0 );
////				access2.bck( 0 );
//				
//			}
			
		}
		
		// variances[0] = 0.0;
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
		

	final double[] param = new double[] { 0.0, 2.0 };
	
	
		final BellCurve func  = new BellCurve();
		
		final double[] arr = new double[ 3 * 3 ];
		final ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles( arr,  3, 3 );
		
		final ArrayCursor<DoubleType> cursor = img.cursor();
		while( cursor.hasNext() ) {
			cursor.fwd();
			double delta = Math.abs( cursor.getDoublePosition( 1 ) - cursor.getDoublePosition( 0 ) );
			if ( delta > 0.1 && ( cursor.getDoublePosition(0) == 2 || cursor.getDoublePosition(1 ) == 2 ) ) {
				delta += 0.5;
			}
			cursor.get().set( func.value( delta, param) );
		}
		
		final double[] lut = new double[] { 0, 1.0, 2.6 }; 
		final LUTRealTransform lutTransform = new LUTRealTransform( lut, 2, 2 );
		
		final ArrayImg<DoubleType, DoubleArray> res = ArrayImgs.doubles( 4, 4 );
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendBorder(img), new NearestNeighborInterpolatorFactory<DoubleType>() );
		LUTRealTransform.render( source, res, lutTransform, 0.1 );
		
		final RealRandomAccessible<DoubleType> view = Views.interpolate( Views.extendBorder( res ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealRandomAccessible<DoubleType> target = new RealTransformRandomAccessible<DoubleType, RealTransform>( source, lutTransform );
		
		for ( int i = 0; i < 3; ++i ) {
			for ( int j = 0; j < 3; ++j ) {
				final RandomAccess<DoubleType> ra = Views.interval( Views.raster( view ), new long[] {0, 0}, new long[]{ 3, 3 } ).randomAccess();
				ra.setPosition( new int[] { i, j } );
			}
		}
		
		final int nCorrs = 100;
		final int nRel   = 7;
		
		final double[] correlationMatrix = new double[ nCorrs * nCorrs ];
		for (int i = 0; i < correlationMatrix.length; i++) {
			correlationMatrix[i] = Double.NaN;
		}
		
		final ArrayImg<DoubleType, DoubleArray> cImage = ArrayImgs.doubles( correlationMatrix, nCorrs, nCorrs );
		final ArrayRandomAccess<DoubleType> cAccess    = cImage.randomAccess();
		
		final Random rng = new Random( nCorrs );
		
		final double[] params = new double[] { 0.0, 2.0 };
		
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
		
		final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( nCorrs );
		
		for ( final DoubleType w : weights ) {
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
