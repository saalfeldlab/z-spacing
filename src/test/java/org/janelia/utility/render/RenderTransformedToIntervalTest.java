/**
 * 
 */
package org.janelia.utility.render;

import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.SingleDimensionLUTRealTransform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class RenderTransformedToIntervalTest {
	
	final Random rng                                 = new Random();
	final int width                                  = 10;
	final int height                                 = 20;
	final int size                                   = 30;
	final double[] data                              = new double[ width*height*size ];
	final ArrayImg< DoubleType, DoubleArray > source = ArrayImgs.doubles( data, width, height, size );
	final double[] lut                               = new double[ size ];
	final int nThreads                               = Runtime.getRuntime().availableProcessors();
	
	@Before
	public void setUp() {
		for ( int i = 0; i < data.length; ++i )
			data[ i ] = rng.nextDouble();
		for ( int i = 0; i < lut.length; ++i )
			lut[i] = 1.5 * i;
	}

	@Test
	public void test() {
		final SingleDimensionLUTRealTransform transform = new SingleDimensionLUTRealTransform( lut, 3, 3, 2 );
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> transformed = RealViews.transformReal( 
				Views.interpolate( Views.extendValue( source, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>() ), 
				transform );
		final IterableInterval<DoubleType> reference    = Views.flatIterable( Views.interval( Views.raster( transformed ), source ) );
		final ArrayImg<DoubleType, DoubleArray> compare = ArrayImgs.doubles( width, height, size );
		
		RenderTransformedToInterval.render( transformed, compare, nThreads );
		
		Cursor<DoubleType> r      = reference.cursor();
		ArrayCursor<DoubleType> c = compare.cursor();
		
		while( c.hasNext() ) {
			Assert.assertTrue( r.hasNext() );
			final double rVal = r.next().get();
			final double cVal = c.next().get();
			Assert.assertEquals( rVal, cVal, 0.0 );
		}
	
	}

}
