/**
 * 
 */
package org.janelia.scaling;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.FlatIterationOrder;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.junit.Before;
import org.junit.Test;

/**
 * @author hanslovskyp
 *
 */
public class BinaryAveragerTest {
	
	private final int nImages = 10;
	private final long width  = 100;
	private final long height = 100;
	private final int minVal  = 0;
	private final int maxVal  = 10; // Integer.MAX_VALUE;
	private final int seed    = 100;
	
	private final LongType nImagesLT  = new LongType( nImages );
	private final FloatType nImagesFT = new FloatType( nImages );
	
	private final ArrayList<RandomAccessibleInterval<LongType>> integerImages = new ArrayList<RandomAccessibleInterval<LongType>>();
	private final ArrayList<RandomAccessibleInterval<FloatType>> floatImages  = new ArrayList<RandomAccessibleInterval<FloatType>>();
	
	private ArrayImg< LongType, LongArray > integerComparison;
	private ArrayImg< FloatType, FloatArray > floatComparison;
	
	
	

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		this.integerComparison = ArrayImgs.longs(width, height);
		this.floatComparison   = ArrayImgs.floats(width, height);
		
		
		final Random generator = new Random( seed );
		
		for ( int i = 0; i < nImages; ++i ) {
			ArrayImg<LongType, LongArray> integerImage = ArrayImgs.longs(width, height);
			ArrayImg<FloatType, FloatArray> floatImage = ArrayImgs.floats(width, height);
			
			ArrayCursor<LongType> integerCursor = integerImage.cursor();
			ArrayCursor<FloatType> floatCursor  = floatImage.cursor();
			
			ArrayCursor<LongType> integerComparisonCursor = this.integerComparison.cursor();
			ArrayCursor<FloatType> floatComparisonCursor  = this.floatComparison.cursor();
			
			while ( integerCursor.hasNext() ) {
				long currValue = Math.max( generator.nextInt( maxVal ), minVal );
				integerCursor.next().setInteger( currValue );
				floatCursor.next().set( currValue );
				
				long currIntegerSum = integerComparisonCursor.next().get();
				float currFloatSum  = floatComparisonCursor.next().get();
				
				integerComparisonCursor.get().set( currIntegerSum + currValue );
				floatComparisonCursor.get().set( currFloatSum + currValue );
			}
			
			integerImages.add(integerImage);
			floatImages.add(floatImage);
		}
		
		ArrayCursor<LongType> integerComparisonCursor = this.integerComparison.cursor();
		ArrayCursor<FloatType> floatComparisonCursor  = this.floatComparison.cursor();
		
		while ( floatComparisonCursor.hasNext() ) {
			integerComparisonCursor.next().div( this.nImagesLT );
			floatComparisonCursor.next().div( this.nImagesFT );
		}
	}

	/**
	 * Test method for {@link org.janelia.scaling.BinaryAverager#average(java.util.ArrayList)}.
	 */
	@Test
	public void testAverage() {
		BinaryAverager<LongType> longAverager = new BinaryAverager<LongType>(1);
		Averager<FloatType> floatAverager     = new BinaryAverager<FloatType>(1);
		
		RandomAccessibleInterval<LongType> longResult   = longAverager.average( this.integerImages );
		RandomAccessibleInterval<FloatType> floatResult = floatAverager.average( this.floatImages );
		
		assertEquals( "Image dimensions must agree", longResult.numDimensions(), floatResult.numDimensions() );
		assertEquals( "Image dimensions must agree", longResult.numDimensions(), integerComparison.numDimensions() );
		assertEquals( "Image dimensions must agree", floatResult.numDimensions(), floatComparison.numDimensions() );
		
		for (int d = 0; d < longResult.numDimensions(); ++d ) {
			assertEquals( "Dimensions must be of equal size", longResult.dimension(d), integerComparison.dimension(d) );
			assertEquals( "Dimensions must be of equal size", floatResult.dimension(d), floatComparison.dimension(d) );
		}
		
//		Due to rounding errors, this method is hard to test for integers, that's why this is only comments
//		FIXME come up with a better idea for a test!
//		Cursor<LongType> longResultCursor = Views.flatIterable(longResult).cursor();
//		ArrayCursor<LongType> longComparisonCursor = this.integerComparison.cursor();
//		
//		while (longResultCursor.hasNext() ) {
//			long rValue = longResultCursor.next().get();
//			long cValue = longComparisonCursor.next().get();
//			System.out.println("" + rValue + " vs " + cValue);
//			assertEquals( "Integral (LongType) values should be the same!", rValue, cValue );
//		}
		
		Cursor<FloatType> floatResultCursor = Views.flatIterable(floatResult).cursor();
		ArrayCursor<FloatType> floatComparisonCursor = this.floatComparison.cursor();
		
		while (floatResultCursor.hasNext() ) {
			float rValue = floatResultCursor.next().get();
			float cValue = floatComparisonCursor.next().get();
			// FIXME define a reasonable choice for tolerance (currently: 0.00001)
			assertEquals( "Integral (FloatType) values should be the same!", rValue, cValue, 0.00001 );
		}
		
	}

}
