/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2023 Howard Hughes Medical Institute.
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

import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.Localizable;
import net.imglib2.Positionable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class AbstractPerumtationTransformTest {
	
	/**
	 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
	 * Wrapper class for AbstractPermutatioTransform to test protected methods.
	 */
	public static class MockPermutation extends AbstractPerumtationTransform {
		
		// test protected methods
		@Override
		public int apply( final int x ) {
			return super.apply(x);
		}
		
		@Override
		public long applyChecked( final int x ) {
			return super.applyChecked( x );
		}
		
		@Override
		public int applyInverse( final int y ) {
			return super.applyInverse( y );
		}
		
		@Override
		public long applyInverseChecked( final int y ) {
			return super.applyInverseChecked( y );
		}

		public MockPermutation(final int[] lut) {
			super(lut);
		}
		
		// ignore unimplemented methods
		@Override
		public void applyInverse(final long[] source, final long[] target) {
			
		}

		@Override
		public void applyInverse(final int[] source, final int[] target) {
		}

		@Override
		public void applyInverse(final Positionable source, final Localizable target) {
		}

		@Override
		public MockPermutation inverse() {
			return new MockPermutation( inverseLut );
		}

		@Override
		public int numSourceDimensions() {
			return 0;
		}

		@Override
		public int numTargetDimensions() {
			return 0;
		}

		@Override
		public void apply(final long[] source, final long[] target) {
			
		}

		@Override
		public void apply(final int[] source, final int[] target) {
			
		}

		@Override
		public void apply(final Localizable source, final Positionable target) {
			
		}
		
	}
	
	private final int size = 200;
	private final int[] lut = new int[ size ];
	private final int[] inv = new int[ size ];

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		final ArrayList<Integer> al = new ArrayList< Integer >();
		for ( int i = 0; i < size; ++i )
			al.add( i );

		Collections.shuffle( al );
		
		for ( int i = 0; i < size; ++i ) {
			lut[ i ]        = al.get( i );
			inv[ lut[ i ] ] = i; 
		}
		
	}

	@Test
	public void test() {
		final MockPermutation transform = new MockPermutation( lut );
		final MockPermutation inverse   = transform.inverse();
		int resInt;
		long resLong;
		for ( int i = 0; i < size; ++i ) {
			resInt  = transform.apply( i );
			resLong = transform.applyChecked( i );
			Assert.assertEquals( lut[ i ], resInt );
			Assert.assertEquals( lut[ i ], resLong );
			
			resInt  = inverse.apply( transform.apply( i ) );
			resLong = transform.apply( inverse.apply( i ) );
			Assert.assertEquals( i, resInt );
			Assert.assertEquals( i, resLong );
			
			resInt  = inverse.apply( i );
			resLong = inverse.applyChecked( i );
			Assert.assertEquals( inv[ i ], resInt );
			Assert.assertEquals( inv[ i ], resLong );
		}
		
		Assert.assertEquals( -Long.MAX_VALUE, transform.applyChecked( -1 ) );
		Assert.assertEquals(  Long.MAX_VALUE, transform.applyChecked( size + 1 ) );
		
	}

}
