/**
 * 
 */
package org.janelia.utility.realtransform;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class MatrixToStripTest {
	
	private final int width = 50;
	private final int range = 10;
	private final ArrayImg< DoubleType, DoubleArray > matrix = ArrayImgs.doubles( width, width );
	double[] lut = new double[ width + 1 ];
	double nonValue  = Double.NaN;
	double fillValue = 0.0;
	
	public static double func( final double diff ) {
		return 1.0 / ( 1.0 + Math.abs( diff ) );
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		for ( final ArrayCursor<DoubleType> c = matrix.cursor(); c.hasNext(); ) {
			c.fwd();
			final int x = c.getIntPosition( 0 );
			final int y = c.getIntPosition( 1 );
			final int diff = Math.abs( x - y );
			final double val = func( diff ); 
			c.get().set( diff <= range? val : nonValue );
		}
		for (int i = 0; i < lut.length; i++) {
			lut[i] = i;
		}
			
	}

	@Test
	public void test() {
		
		final RandomAccessibleInterval<DoubleType> strip = MatrixToStrip.toStrip( matrix, range, new DoubleType( fillValue ) );
		
		for ( final Cursor<DoubleType> cursor = Views.flatIterable( strip ).cursor(); cursor.hasNext(); ) {
			cursor.fwd();
			final int x = cursor.getIntPosition( 0 );
			final int y = cursor.getIntPosition( 1 );
			final double val  = cursor.get().get();
			final double diff = x - range;
			if ( y + diff < 0 || y + diff >= strip.dimension( 1 ) ) {
				if ( Double.isNaN( fillValue ) )
					Assert.assertTrue( Double.isNaN( val ) );
				else
					Assert.assertEquals( fillValue, val, 0.0 );
			} else
				Assert.assertEquals( func( diff ), val, 0.0 );
			
		}
		
	}

}
