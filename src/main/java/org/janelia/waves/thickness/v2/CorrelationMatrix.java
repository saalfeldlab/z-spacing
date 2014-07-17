package org.janelia.waves.thickness.v2;

import java.util.Map.Entry;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;

public class CorrelationMatrix {
	
	private final ArrayImg< DoubleType, DoubleArray > correlationValues;

	public CorrelationMatrix(ArrayImg<DoubleType, DoubleArray> correlationValues) {
		super();
		this.correlationValues = correlationValues;
	}
	
	public RealTransformRandomAccessible<DoubleType, RealTransform> shiftCoordinates( LUTRealTransform transform, InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory ) {
		return new RealTransformRandomAccessible<DoubleType, RealTransform>( Views.interpolate( Views.extendBorder( this.correlationValues ), interpolatorFactory ), transform);
	}
	
	public static CorrelationMatrix createFromCorrelationsObjectAtXY( CorrelationsObjectInterface correlationsObject, long x, long y ) {
		
		long zMin = Long.MAX_VALUE;
		long zMax = Long.MIN_VALUE;
		// determine size of matrix
		for ( Entry<Long, Meta> entry : correlationsObject.getMetaMap().entrySet() ) {
			
			if ( entry.getValue().zCoordinateMin < zMin ) {
				zMin = entry.getValue().zCoordinateMin;
			}
			
			if ( entry.getValue().zCoordinateMax > zMax ) {
				zMax = entry.getValue().zCoordinateMax;
			}
			
		}
		
		int nEntries = (int) (zMax - zMin);
		
		double[] correlationArray = new double[ nEntries * nEntries ];
		
		for (int i = 0; i < correlationArray.length; ++i) {
			correlationArray[i] = Double.NaN;
		}
		
		ArrayImg<DoubleType, DoubleArray> correlations = ArrayImgs.doubles( correlationArray, nEntries, nEntries );
		ArrayRandomAccess<DoubleType> ra = correlations.randomAccess();
		
		for ( Entry<Long, Meta> entry : correlationsObject.getMetaMap().entrySet() ) {
			ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> correlationsAtZ = correlationsObject.extractDoubleCorrelationsAt( x, y, entry.getValue().zPosition );
			
			Cursor<DoubleType> corr  = Views.iterable( correlationsAtZ.getA() ).cursor();
			
			long zStart = entry.getValue().zCoordinateMin;
			
			ra.setPosition( entry.getValue().zPosition, 0 );
			
			while ( corr.hasNext() ) {
				ra.setPosition( zStart - zMin, 1 );
				++zStart;
			}
		}
		
		return new CorrelationMatrix( correlations );
	}
	
	
	

	
	public static void main( String[ ]args ) throws FunctionEvaluationException {
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
			System.out.println( cursor.getIntPosition(0) + "," + cursor.getIntPosition(1) + ": " + delta + "," + func.value( delta, param) );
			cursor.get().set( func.value( delta, param) );
		}
		
		CorrelationMatrix matrix = new CorrelationMatrix( img );
		
		double[] lut = new double[] { 0, 1, 1.5 }; 
		LUTRealTransform lutTransform = new LUTRealTransform( lut, 2, 2 );
		
		RandomAccessible<DoubleType> shifted = matrix.shiftCoordinates( lutTransform, new NLinearInterpolatorFactory<DoubleType>() );// new NearestNeighborInterpolatorFactory<DoubleType>());
		IntervalView<DoubleType> shiftedInterval = Views.interval( shifted, new FinalInterval( 3, 3 )  );
		
		Cursor<DoubleType> siCursor = Views.flatIterable( shiftedInterval ).cursor();
		
		while ( siCursor.hasNext() ) {
			siCursor.fwd();
			System.out.println( siCursor.getDoublePosition(0) + "," + siCursor.getDoublePosition(1) + ": " + siCursor.get().get());
		}
		
	}
	
}
