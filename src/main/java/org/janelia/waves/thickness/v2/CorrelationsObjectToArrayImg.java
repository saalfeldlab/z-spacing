package org.janelia.waves.thickness.v2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class CorrelationsObjectToArrayImg {
	
	public static final int Z_AXIS  = 1;
	public static final int DZ_AXIS = 1 - Z_AXIS;
	
	ConstantPair< ArrayImg< DoubleType, DoubleArray >, ArrayImg< DoubleType, DoubleArray > > extract( CorrelationsObjectInterface correlationsObject, long x, long y ) {
		long zRangeLower = 0;
		long zRangeUpper = 0;
		long zBinMin     = Integer.MAX_VALUE;
		long zBinMax     = Integer.MIN_VALUE;
		
		
		// get image size
		TreeMap<Long, Meta> metaMap = correlationsObject.getMetaMap();
		
		for ( Entry<Long, Meta> entry : metaMap.entrySet() ) {
			
			if ( entry.getValue().zPosition < zBinMin ) {
				zBinMin = entry.getKey();
			}
			
			if ( entry.getValue().zPosition > zBinMax ) {
				zBinMax = entry.getKey();
			}
			
			if ( entry.getValue().zPosition - entry.getValue().zCoordinateMin > zRangeLower ) {
				zRangeLower = entry.getValue().zPosition - entry.getValue().zCoordinateMin;
			}
			
			if ( entry.getValue().zCoordinateMax - entry.getValue().zPosition > zRangeUpper ) {
				zRangeUpper = entry.getValue().zCoordinateMax - entry.getValue().zPosition;
			}
		}
		
		// extract correlations and write into image
		double[] correlationsArray = new double[ (int) (( zRangeLower + zRangeUpper ) * ( zBinMax - zBinMin )) ];
		Arrays.fill( correlationsArray, Double.NaN );
		ArrayImg<DoubleType, DoubleArray> correlations = ArrayImgs.doubles( correlationsArray, zRangeLower + zRangeUpper, zBinMax - zBinMin );
		ArrayImg<DoubleType, DoubleArray> coordinates  = ArrayImgs.doubles( zBinMax - zBinMin );
		
		ArrayCursor<DoubleType> coordinateCursor = coordinates.cursor();
		
		
		for ( Entry<Long, Meta> entry : metaMap.entrySet() ) {
			ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > correlationsAndCoordinates = correlationsObject.extractDoubleCorrelationsAt( x, y, entry.getKey() );
			IntervalView<DoubleType> currentCorrelationSlice = Views.hyperSlice( correlations, Z_AXIS, entry.getValue().zPosition - zBinMin );
			
			long startingIndex = zRangeLower - ( entry.getValue().zPosition - entry.getValue().zCoordinateMin ); // not all zPositions have the maximum correlation range
			
			Cursor<DoubleType> targetCursor = Views.flatIterable( currentCorrelationSlice ).cursor();
			Cursor<DoubleType> sourceCursor = Views.flatIterable( correlationsAndCoordinates.getA() ).cursor();
			
			targetCursor.jumpFwd( startingIndex );
			
			while ( sourceCursor.hasNext() ) {
				targetCursor.next().set( sourceCursor.next() );
			}
			
			coordinateCursor.next().set( entry.getValue().zPosition );
			
		}
		
		return new ConstantPair< ArrayImg< DoubleType, DoubleArray >, ArrayImg< DoubleType, DoubleArray > >( coordinates, correlations );
	}
}
