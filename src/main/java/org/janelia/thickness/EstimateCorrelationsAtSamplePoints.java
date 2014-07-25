package org.janelia.thickness;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
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

import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.janelia.thickness.lut.LUTRealTransform;

public class EstimateCorrelationsAtSamplePoints {
	
	private final static float[] ONE_DIMENSION_ZERO_POSITION = new float[] { 0.0f };
	
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

}
