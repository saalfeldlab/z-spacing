package org.janelia.scaling;

import static org.junit.Assert.*;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.waves.thickness.ConstantPair;
import org.junit.Before;
import org.junit.Test;

public class SingleImageBinaryAveragerTest {
	
	private final SingleImageBinaryAverager<FloatType> floatAverager;
	
	private final ArrayImg<LongType, LongArray > longImage;
	private final ArrayImg<FloatType, FloatArray> floatImage;
	
	private final ArrayList<ConstantPair<Long, Long>> stepListXReference = new ArrayList<ConstantPair<Long, Long>>();
	private final ArrayList<ConstantPair<Long, Long>> stepListYReference = new ArrayList<ConstantPair<Long, Long>>();
	
	private final ArrayImg<FloatType, FloatArray> resultImageReference;
	private final ArrayList< RandomAccessibleInterval<FloatType> > validInputList;
	private final ArrayList< RandomAccessibleInterval<FloatType> > emptyInputList;
	private final ArrayList< RandomAccessibleInterval<FloatType> > tooManyInputList;
	
	private final long step = 3;
	
	

	public SingleImageBinaryAveragerTest() {
		super();
		new SingleImageBinaryAverager<LongType>( step, 1 );
		floatAverager = new SingleImageBinaryAverager<FloatType>( step, 1 );
		
		longImage  = ArrayImgs.longs( 4, 6);
		floatImage = ArrayImgs.floats( 4, 6 );
		
		resultImageReference = ArrayImgs.floats( 2, 2);
		validInputList       = new ArrayList< RandomAccessibleInterval<FloatType> >();
		emptyInputList       = new ArrayList< RandomAccessibleInterval<FloatType> >();
		tooManyInputList     = new ArrayList< RandomAccessibleInterval<FloatType> >();
	}

	@Before
	public void setUp() throws Exception {
		 ArrayCursor<LongType> longCursor   = longImage.cursor();
		 ArrayCursor<FloatType> floatCursor = floatImage.cursor();
		 
		 long value = 1;
		 
		 while ( longCursor.hasNext() ) {
			 longCursor.next().set( value );
			 floatCursor.next().set( value );
			 ++value;
		 }
		 
		 /*
		  * 
		  *    1   2   3 | 4
		  *    5   6   7 | 8
		  *    9  10  11 |12
		  *   --------------
		  *   13  14  15 |16
		  *   17  18  19 |20
		  *   21  22  23 |24
		  *   
		  *   We set up an image like above, where --- and | indictate the partitions by step size. the resulting image
		  *   will be a 2 x 2 image with the following entries:
		  *     upper left  =  6
		  *     upper right =  8
		  *     lower left  = 18
		  *     lower right = 20
		  *       
		  */
		 
		 float[] averages = new float[]{ 6.f, 8.f, 18.f, 20.f };
		 ArrayCursor<FloatType> cursor = resultImageReference.cursor();
		 for (float a : averages) {
			 cursor.next().set( a );
		 }
		 
		 validInputList.add( floatImage );
		 tooManyInputList.add( floatImage );
		 tooManyInputList.add( floatImage );
		 
		 
		 
		 stepListXReference.add( new ConstantPair<Long, Long>( new Long(0), new Long(3)) );
		 stepListXReference.add( new ConstantPair<Long, Long>( new Long(3), new Long(4)) );
		 
		 stepListYReference.add( new ConstantPair<Long, Long>( new Long(0), new Long(3)) );
		 stepListYReference.add( new ConstantPair<Long, Long>( new Long(3), new Long(6)) );
		 
	}

	@Test
	public void testAverage() {
		
		RandomAccessibleInterval<FloatType> resultImage = floatAverager.average( validInputList );
		
		assertEquals( resultImageReference.numDimensions(), resultImage.numDimensions());
		
		for ( int d = 0; d < resultImage.numDimensions(); ++d ) {
			assertEquals( resultImageReference.dimension(d), resultImage.dimension(d));
		}
		
		
		Cursor<FloatType> resultCursor         = Views.flatIterable( resultImage ).cursor();
		ArrayCursor<FloatType> referenceCursor = resultImageReference.cursor();
		
		while ( resultCursor.hasNext() ) {
			 assertEquals( referenceCursor.next().getRealFloat(), resultCursor.next().getRealFloat(), 1);
		}
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void testIllegalArgumentExceptionEmptyInput() {
		floatAverager.average( emptyInputList );
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void testIllegalArgumentExceptionTooManyInput() {
		floatAverager.average( tooManyInputList );
	}

	@Test
	public void testGenerateStepList() {
		long xMin = 0;
		long xMax = longImage.dimension( 0 );
		
		long yMin = 0;
		long yMax = longImage.dimension( 1 );
		
		ArrayList<ConstantPair<Long, Long>> stepListX = SingleImageBinaryAverager.generateStepList( xMin, xMax, step );
		ArrayList<ConstantPair<Long, Long>> stepListY = SingleImageBinaryAverager.generateStepList( yMin, yMax, step );
		
		
		for ( int i = 0; i < stepListX.size(); ++i ) {
			assertEquals( stepListXReference.get(i).getA(), stepListX.get(i).getA() );
			assertEquals( stepListXReference.get(i).getB(), stepListX.get(i).getB() );
		}
		
		for ( int i = 0; i < stepListY.size(); ++i ) {
			assertEquals( stepListYReference.get(i).getA(), stepListY.get(i).getA() );
			assertEquals( stepListYReference.get(i).getB(), stepListY.get(i).getB() );
		}
		
	}
	

	@Test
	public void testBinarySumAverage() {
		FloatType result = floatAverager.binarySumAverage( floatImage );
		// sum_(i=1)^24 i = 24 * 25 / 2 = 300
		// 300 / 24 = 12.5
		assertEquals( 12.5, result.getRealDouble(), 0.0000001 );
	}

}
