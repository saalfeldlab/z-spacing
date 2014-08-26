/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImgs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SingleDimensionLUTGridTest {
	
	private final double[] sourceDouble = new double[] { 0.0, 0.0, 2.0 };
	private final float[] sourceFloat   = new float[] { 0.0f, 0.0f, 1.0f };
	private final double[] sourceOffGrid = new double[] { 0.0, 0.0, 2.6 };
	private final RealPoint sourceRealPoint = new RealPoint( sourceOffGrid );
	private final double[] targetDouble = new double[] { 0.0, 0.0, 5.5 };
	private final float[] targetFloat  = new float[] { 0.0f, 0.0f, 3.5f };
	private final double[] targetOffGrid = new double[] { 0.0, 0.0, 5.8 };
	private final RealPoint targetRealPoint = new RealPoint( targetOffGrid );
	
	private final double[] DoubleSourceDouble = sourceDouble.clone();
	private final float[] DoubleSourceFloat   = sourceFloat.clone();
	private final RealPoint DoubleSourceRealPoint = new RealPoint( sourceOffGrid.clone() );
	
	private final double[] DoubleTargetDouble = targetDouble.clone();
	private final float[] DoubleTargetFloat   = targetFloat.clone();
	private final RealPoint DoubleTargetRealPoint = new RealPoint( targetOffGrid.clone() );
	
	
	// 3 x 3 x 4 transform lut with same entries all over the place
	private final SingleDimensionLUTGrid tf1 = new SingleDimensionLUTGrid( 3, 3, ArrayImgs.doubles( new double[] { 1.0, 2.0, 5.5, 6.0, 
			1.0, 2.0, 5.5, 6.0, 
			1.0, 2.0, 5.5, 6.0,
			1.0, 2.0, 5.5, 6.0,
			1.0, 2.0, 5.5, 6.0,
			1.0, 2.0, 5.5, 6.0,
			1.0, 2.0, 5.5, 6.0,
			1.0, 2.0, 5.5, 6.0
			,1.0, 2.0, 5.5, 6.0}, 3, 3, 4 ), 2 );
	private final SingleDimensionLUTGrid tf2 = new SingleDimensionLUTGrid( 3, 3, ArrayImgs.doubles( new double[] { 1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0,
			1.0, 3.5, 4.5, 5.0}, 3, 3, 4 ), 2 );	

	final double s1 = 2.0;
	final double s2 = 3.0;
	
	final SingleDimensionLUTGrid copy1 = tf1.reScale( 1./s1 );
	final SingleDimensionLUTGrid copy2 = tf2.reScale( 1./s2 );
	
	
	private final double[] resultDouble = new double[ 3 ];
	private final float[] resultFloat = new float[ 3 ];
	private final RealPoint resultRealPoint = new RealPoint( 3 );
	
	
	@Before
	public void setUp() {
		
		for ( int i = 0; i < 2; ++i ) {
			DoubleSourceDouble[i] *= s1;
			DoubleSourceFloat[i]  *= s2;
			DoubleSourceRealPoint.setPosition( s1*DoubleSourceRealPoint.getDoublePosition(i), i);
			
			DoubleTargetDouble[i] *= s1;
			DoubleTargetFloat[i]  *= s2;
			DoubleTargetRealPoint.setPosition( s1*DoubleTargetRealPoint.getDoublePosition(i), i);
		}
	}
	

	@Test
	public void test() {
		tf1.apply( sourceDouble, resultDouble );
		tf1.apply( sourceRealPoint, resultRealPoint );
		tf2.apply( sourceFloat, resultFloat );
		
		Assert.assertArrayEquals( targetDouble, resultDouble, 0.0 );
		Assert.assertArrayEquals( targetFloat, resultFloat, 0.0f );
		for ( int d = 0; d < targetRealPoint.numDimensions(); ++ d )
		{
			Assert.assertEquals( targetRealPoint.getDoublePosition(d), resultRealPoint.getDoublePosition(d), 0.0 );
		}
		
		
		copy1.apply( DoubleSourceDouble, resultDouble );
		copy1.apply( DoubleSourceRealPoint, resultRealPoint );
		copy2.apply( DoubleSourceFloat, resultFloat );
		
		Assert.assertArrayEquals( DoubleTargetDouble, resultDouble, 0.0 );
		Assert.assertArrayEquals( DoubleTargetFloat, resultFloat, 0.0f );
		for ( int d = 0; d < DoubleTargetRealPoint.numDimensions(); ++ d )
		{
			Assert.assertEquals( DoubleTargetRealPoint.getDoublePosition(d), resultRealPoint.getDoublePosition(d), 0.0 );
		}
	}
	
	@Test
	public void testInverse() {
		tf1.applyInverse( resultDouble, targetDouble );
		tf1.applyInverse( resultRealPoint, targetRealPoint );
		tf2.applyInverse( resultFloat, targetFloat );
		
		Assert.assertArrayEquals( sourceDouble, resultDouble, 0.0 );
		Assert.assertArrayEquals( sourceFloat, resultFloat, 0.0f );
		for ( int d  =  0; d < sourceRealPoint.numDimensions(); ++d ) {
			Assert.assertEquals(sourceRealPoint.getDoublePosition( d ), resultRealPoint.getDoublePosition( d ), 0.0000000001 );
		}
		
		
		copy1.applyInverse( resultDouble, DoubleTargetDouble );
		copy1.applyInverse( resultRealPoint, DoubleTargetRealPoint );
		copy2.applyInverse( resultFloat, DoubleTargetFloat );
		
		Assert.assertArrayEquals( DoubleSourceDouble, resultDouble, 0.0 );
		Assert.assertArrayEquals( DoubleSourceFloat, resultFloat, 0.0f );
		for ( int d  =  0; d < DoubleSourceRealPoint.numDimensions(); ++d ) {
			Assert.assertEquals( DoubleSourceRealPoint.getDoublePosition( d ), resultRealPoint.getDoublePosition( d ), 0.0000000001 );
		}
	}

}
