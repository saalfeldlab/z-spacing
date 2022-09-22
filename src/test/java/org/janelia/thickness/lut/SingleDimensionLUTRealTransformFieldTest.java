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

import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImgs;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SingleDimensionLUTRealTransformFieldTest {

	
	private final double[] sourceDouble = new double[] { 0.0, 0.0, 2.0 };
	private final float[] sourceFloat   = new float[] { 0.0f, 0.0f, 1.0f };
	private final double[] sourceOffGrid = new double[] { 0.0, 0.0, 2.6 };
	private final RealPoint sourceRealPoint = new RealPoint( sourceOffGrid );
	private final double[] targetDouble = new double[] { 0.0, 0.0, 5.5 };
	private final float[] targetFloat  = new float[] { 0.0f, 0.0f, 3.5f };
	private final double[] targetOffGrid = new double[] { 0.0, 0.0, 5.8 };
	private final RealPoint targetRealPoint = new RealPoint( targetOffGrid );
	
	private final SingleDimensionLUTRealTransformField tf1 = new SingleDimensionLUTRealTransformField( 3, 3, ArrayImgs.doubles( new double[] { 1.0, 2.0, 5.5, 6.0 }, 1, 1, 4 ) );
	private final SingleDimensionLUTRealTransformField tf2 = new SingleDimensionLUTRealTransformField( 3, 3, ArrayImgs.doubles( new double[] { 1.0, 3.5, 4.5, 5.0 }, 1, 1, 4 ) );
	
	private final double[] resultDouble = new double[ 3 ];
	private final float[] resultFloat = new float[ 3 ];
	private final RealPoint resultRealPoint = new RealPoint( 3 );
	


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
	}

}
