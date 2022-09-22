/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.Cursor;
import net.imglib2.RealPoint;
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
public class SingleDimensionLUTGridTest {
	
	private final double[] sourceDouble = new double[] { 1.0, 1.0, 2.0 };
	private final float[] sourceFloat   = new float[] { 1.0f, 1.0f, 1.0f };
	private final double[] sourceOffGrid = new double[] { 1.0, 1.0, 2.6 };
	private final RealPoint sourceRealPoint = new RealPoint( sourceOffGrid );
	private final double[] targetDouble = new double[] { 1.0, 1.0, 5.5 };
	private final float[] targetFloat  = new float[] { 1.0f, 1.0f, 3.5f };
	private final double[] targetOffGrid = new double[] { 1.0, 1.0, 5.8 };
	private final RealPoint targetRealPoint = new RealPoint( targetOffGrid );
	
	private final double[] DoubleSourceDouble = sourceDouble.clone();
	private final float[] DoubleSourceFloat   = sourceFloat.clone();
	private final RealPoint DoubleSourceRealPoint = new RealPoint( sourceOffGrid.clone() );
	
	private final double[] DoubleTargetDouble = targetDouble.clone();
	private final float[] DoubleTargetFloat   = targetFloat.clone();
	private final RealPoint DoubleTargetRealPoint = new RealPoint( targetOffGrid.clone() );
	
	
	// 3 x 3 x 4 transform lut with same entries all over the place
	private SingleDimensionLUTGrid tf1;
	private SingleDimensionLUTGrid tf2;	

	final double s1 = 2.0;
	final double s2 = 3.0;
	
	private SingleDimensionLUTGrid copy1;
	private SingleDimensionLUTGrid copy2;
	
	
	private final double[] resultDouble = new double[ 3 ];
	private final float[] resultFloat = new float[ 3 ];
	private final RealPoint resultRealPoint = new RealPoint( 3 );
	
	private final ArrayImg< DoubleType, DoubleArray> lut1 = ArrayImgs.doubles( 3, 3, 4 );
	private final ArrayImg< DoubleType, DoubleArray> lut2 = ArrayImgs.doubles( 3, 3, 4 );
	
	
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
		
		final double[] arr1 = new double[] { 1.0, 2.0, 5.5, 6.0 };
		final double[] arr2 = new double[] { 1.0, 3.5, 4.5, 5.0 };
		
		final Cursor<DoubleType> c1 = Views.flatIterable( Views.hyperSlice( Views.hyperSlice( lut1, 1, 1), 0, 1) ).cursor();
		final Cursor<DoubleType> c2 = Views.flatIterable( Views.hyperSlice( Views.hyperSlice( lut2, 1, 1), 0, 1) ).cursor();
		
		
		for (int i = 0; i < arr2.length; i++) {
			c1.fwd();
			c2.fwd();
			c1.get().set( arr1[i] );
			c2.get().set( arr2[i] );
		}
		
		tf1 = new SingleDimensionLUTGrid( 3, 3, lut1, 2 );
		tf2 = new SingleDimensionLUTGrid( 3, 3, lut2, 2 );
		
		copy1 = tf1.reScale( s1 );
		copy2 = tf2.reScale( s2 );
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
