package org.janelia.correlations;

import java.util.Random;

import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CrossCorrelationTest {
	
	private final long[] dim = new long[] { 4,5 };
	private final long[] randomDim = new long[] { (long) 1e6 };
	private final ArrayImg< DoubleType, DoubleArray > img1 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img2 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img3 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img4 = ArrayImgs.doubles( randomDim );
	private final ArrayImg< DoubleType, DoubleArray > img5 = ArrayImgs.doubles( randomDim );
	private final Random rng = new Random( 100 );

	@Before
	public void setUp() throws Exception {
		
		final ArrayCursor<DoubleType> c1 = img1.cursor();
		final ArrayCursor<DoubleType> c2 = img2.cursor();
		final ArrayCursor<DoubleType> c3 = img3.cursor();
		final ArrayCursor<DoubleType> c4 = img4.cursor();
		final ArrayCursor<DoubleType> c5 = img5.cursor();
		
		for ( int i = 1; c1.hasNext(); ++i ) {
			c1.next().set( i );
			c2.next().set( 0.5*i + 1 );
			c3.next().set( -0.5*i );
		}
		/*
		 * mean1: 10.50
		 * mean2:  6.25
		 * mean3: -5.25
		 */
		while( c4.hasNext() ) {
			c4.next().set( rng.nextDouble() );
			c5.next().set( rng.nextDouble() );
		}
		
	}

	@Test
	public void test() {
		double val1;
		double val2;
		// correlation == 1
		final CrossCorrelation<DoubleType, DoubleType> cc1 = new CrossCorrelation<DoubleType, DoubleType>(img1, img2, new long[] { 2, 2 } );
		final RandomAccess<FloatType> ra1 = cc1.randomAccess();
		ra1.setPosition( new int[] { 2, 2 } );
		val1 = ra1.get().get();
		val2 = ra1.get().get();
		Assert.assertEquals( 1.0, val1, 0.00000000001 );
		Assert.assertEquals( val1, val2, 0.0 );
		
		// correlation == -1
		final CrossCorrelation<DoubleType, DoubleType> cc2 = new CrossCorrelation<DoubleType, DoubleType>(img1, img3, new long[] { 2, 2 } );
		final RandomAccess<FloatType> ra2 = cc2.randomAccess();
		ra2.setPosition( new int[] { 2, 2 } );
		val1 = ra2.get().get();
		val2 = ra2.get().get();
		Assert.assertEquals( -1.0, val1, 0.00000000001 );
		Assert.assertEquals( val1,  val2, 0.0 );
		
		// correlation ~~ 0
		final CrossCorrelation< DoubleType, DoubleType > cc3 = new CrossCorrelation<DoubleType, DoubleType>(img4, img5, randomDim );
		final RandomAccess<FloatType> ra3 = cc3.randomAccess();
		ra3.setPosition( new long[] { randomDim[0] / 2 } );
		val1 = ra3.get().get();
		val2 = ra3.get().get();
		Assert.assertEquals( 0.0, val1, 1e-3 ); 
		/*
		 * 1e-3 is strongly dependent on number of elemetns in img4/img5
		 * and might also be dependent on seed 
		 */
		Assert.assertEquals( val1, val2, 0.0 );
	}

}

