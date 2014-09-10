package org.janelia.correlations;

import ij.process.FloatProcessor;

import java.util.Random;

import mpicbg.ij.integral.BlockPMCC;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CrossCorrelationTest {
	
	private final long[] dim       = new long[] { 4,5 };
	private final long[] randomDim = new long[] { (long) 1e6 };
	private final long[] random2D  = new long[]  { 40, 50 };
	private final float[]   randomFloat1D1 = new float[ (int) (random2D[0] * random2D[1]) ];
	private final float[]   randomFloat1D2 = new float[ (int) (random2D[0] * random2D[1]) ];
	private final float[][] randomFloat2D1 = new float[ (int) random2D[0] ][ (int) random2D[1] ];
	private final float[][] randomFloat2D2 = new float[ (int) random2D[0] ][ (int) random2D[1] ];
	private final float[][] smallExample1  = new float[][] { { 3f, 1f, 3f, 1f } };
	private final float[][] smallExample2  = new float[][] { { 1f, 3f, 2f, 2f } };
	private final ArrayImg< DoubleType, DoubleArray > img1 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img2 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img3 = ArrayImgs.doubles( dim );
	private final ArrayImg< DoubleType, DoubleArray > img4 = ArrayImgs.doubles( randomDim );
	private final ArrayImg< DoubleType, DoubleArray > img5 = ArrayImgs.doubles( randomDim );
	private final ArrayImg< FloatType,  FloatArray  > img6 = ArrayImgs.floats( randomFloat1D1, random2D );
	private final ArrayImg< FloatType,  FloatArray  > img7 = ArrayImgs.floats( randomFloat1D2, random2D );
	private final ArrayImg< FloatType,  FloatArray  > img8 = ArrayImgs.floats( smallExample1[ 0 ], 1, smallExample1[ 0 ].length );
	private final ArrayImg< FloatType,  FloatArray  > img9 = ArrayImgs.floats( smallExample2[ 0 ], 1, smallExample2[ 0 ].length );
	private final Random rng = new Random( 100 );

	@Before
	public void setUp() throws Exception {
		
		final ArrayCursor<DoubleType> c1 = img1.cursor();
		final ArrayCursor<DoubleType> c2 = img2.cursor();
		final ArrayCursor<DoubleType> c3 = img3.cursor();
		final ArrayCursor<DoubleType> c4 = img4.cursor();
		final ArrayCursor<DoubleType> c5 = img5.cursor();
		final ArrayCursor<FloatType>  c6 = img6.cursor();
		final ArrayCursor<FloatType>  c7 = img7.cursor();
		
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
		
		while ( c6.hasNext() ) {
			c6.fwd();
			c7.fwd();
			final float val1 = rng.nextFloat();
			final float val2 = rng.nextFloat();
			final int x = c6.getIntPosition( 0 );
			final int y = c6.getIntPosition( 1 );
			c6.get().set( val1 );
			c7.get().set( val2 );
			randomFloat2D1[ x ][ y ] = val1;
			randomFloat2D2[ x ][ y ] = val2;
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
	
	
	@Test
	public void testWithBlockPMCC() {
		
		for ( int divisor = 1; divisor <= 10; divisor += 2 ) {
		
			final long[] longRadius = random2D.clone();
			for (int i = 0; i < longRadius.length; i++) {
				longRadius[i] /= divisor;
			}
			final int[]  intRadius  = new int[] { (int) longRadius[0], (int) longRadius[1] };
			
			final FloatProcessor fp1 = new FloatProcessor( randomFloat2D1 );
			final FloatProcessor fp2 = new FloatProcessor( randomFloat2D2 );
			final BlockPMCC blockCC1  = new BlockPMCC( fp1, fp2, 0, 0 ); // offset needs to be explicitly set to 0,0
			final BlockPMCC blockCC2  = new BlockPMCC( fp1, fp2, 0, 0 ); // offset needs to be explicitly set to 0,0
			final CrossCorrelation< FloatType, FloatType > cc1 = new CrossCorrelation< FloatType, FloatType >( img6, img7, longRadius );
			final CrossCorrelation< FloatType, FloatType > cc2 = new CrossCorrelation< FloatType, FloatType >( img6, img7, longRadius, CrossCorrelation.TYPE.SIGNED_SQUARED );
			
			
			blockCC1.r( intRadius[0], intRadius[1] );
			blockCC2.rSignedSquare( intRadius[0], intRadius[1] );
			final FloatProcessor fpRes1 = blockCC1.getTargetProcessor();
			final FloatProcessor fpRes2 = blockCC2.getTargetProcessor();
			
			final Cursor<FloatType> c1 = Views.flatIterable( cc1 ).cursor();
			final Cursor<FloatType> c2 = Views.flatIterable( cc2 ).cursor();
			
			while( c1.hasNext() ) {
				c1.fwd();
				c2.fwd();
				final int x = c1.getIntPosition( 0 );
				final int y = c1.getIntPosition( 1 );
				
				final float cVal1  = c1.get().get();
				final float cVal2  = c2.get().get();
				final float fpVal1 = fpRes1.getf( x, y );
				final float fpVal2 = fpRes2.getf( x, y );
				Assert.assertEquals( fpVal1, cVal1, 1e-6f );
				Assert.assertEquals( fpVal2, cVal2, 1e-6f );
				Assert.assertEquals( cVal1*cVal1, Math.abs( cVal2 ), 1e-6f );
			}
			
		}
		
	}
	
	
	@Test
	public void testSmallExample() {
		
		// smallExample1 = [[ 3, 1, 3, 1 ]]
		// smallExample2 = [[ 1, 3, 2, 2 ]]
		//
		//                    mean         variance
		// smallExample1         2              1.0
		// smallExample2         1              0.5
		//
		// normalized cross-correlation: -sqrt(0.5)
		
		final int radius = 10;
		
		final float reference = (float) (-Math.sqrt( 0.5 ) ); 
		
		final FloatProcessor fp1 = new FloatProcessor( smallExample1 );
		final FloatProcessor fp2 = new FloatProcessor( smallExample2 );
		final BlockPMCC blockCC  = new BlockPMCC( fp1, fp2, 0, 0 );
		final CrossCorrelation<FloatType, FloatType> cc = new CrossCorrelation<FloatType, FloatType>( img8, img9, new long[] { radius } );
		blockCC.r( radius );
		final FloatProcessor fpRes = blockCC.getTargetProcessor();
		
		final Cursor<FloatType> c = Views.flatIterable( cc ).cursor();
		while ( c.hasNext() ) {
			c.fwd();
			final int x = c.getIntPosition( 0 );
			final int y = c.getIntPosition( 1 );
//			System.out.println( reference + " vs " + fpRes.getf( x, y ) + " ~ " + c.get().get() );
			// make sure that both approaches produce the desired result
			Assert.assertEquals( reference, c.get().get(), 0.0f );
			Assert.assertEquals( reference, fpRes.getf( x, y ), 0.0f );
		}
		
		
	
	}

}

