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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SingleDimensionPermutationTransformTest {
	
	private final Random rng = new Random();
	private final int width       = 100;
	private final int height      = 200;
	private final long[] dim      = new long[] { width, height };
	private final int d           = 1;
	private final int size        = (int) dim[ d ];
	private final int nDim        = dim.length;
	private final int[] lut       = new int[ size ];
	private final int[] inv       = new int[ size ];
	private final int nRandomReps = 1000;
	
	private final ArrayImg< IntType, IntArray > img = ArrayImgs.ints( dim );

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		final ArrayList<Integer> al = new ArrayList< Integer >();
		for ( int i = 0; i < size; ++i )
			al.add( i );
		
		Collections.shuffle( al );
		
		for ( int i = 0;  i < lut.length; ++i ) {
			lut[ i ]        = al.get( i );
			inv[ lut[ i ] ] = i;
		}
		
		for ( final IntType l : img )
			l.set( rng.nextInt() );
	}

	
	@Test
	public <T> void test() {
		final SingleDimensionPermutationTransform transform = new SingleDimensionPermutationTransform( lut, nDim, nDim, d );
		final SingleDimensionPermutationTransform inverse   = transform.inverse();

		final TransformView< IntType > transformed = new TransformView< IntType >( img, transform );
		final TransformView< IntType > inversed    = new TransformView< IntType >( transformed, inverse );
		
		final ArrayCursor< IntType > ref = img.cursor();
		final Cursor< IntType > res      = Views.flatIterable( Views.interval( inversed, img ) ).cursor();
		while( ref.hasNext() )
			Assert.assertEquals( ref.next().get(), res.next().get() );

		final ArrayRandomAccess<IntType> raFwd = img.randomAccess();
		final ArrayRandomAccess<IntType> raBck = img.randomAccess();
		final ArrayRandomAccess<IntType> ra    = img.randomAccess();
		
		final long[] fwdLong = new long[ nDim ];
		final long[] bckLong = new long[ nDim ];
		
		final int[] fwdInt = new int[ nDim ];
		final int[] bckInt = new int[ nDim ];
		
		for ( int i = 0; i < nRandomReps; ++i ) {
			final int x = rng.nextInt( width );
			final int y = rng.nextInt( width );
			final int[] xyInt   = new int[] { x, y };
			final long[] xyLong = new long[] { x, y }; 
			ra.setPosition( xyInt );
			
			transform.apply( xyInt, fwdInt );
			transform.apply( xyLong, fwdLong );
			transform.apply( ra, raFwd );
			
			transform.applyInverse( bckInt, xyInt );
			transform.applyInverse( bckLong, xyLong );
			transform.applyInverse( raBck, ra );
			
			for ( int d = 0; d < nDim; ++d ) {
				final int fwdVal;
				final int bckVal;
				if ( d == this.d ) {
					fwdVal = lut[ xyInt[ d ] ];
					bckVal = inv[ xyInt[ d ] ];
				} else {
					fwdVal = xyInt[ d ];
					bckVal = xyInt[ d ];
				}

				Assert.assertEquals( fwdVal, fwdInt[d] );
				Assert.assertEquals( bckVal, bckInt[d] );
				
				Assert.assertEquals( fwdVal, fwdLong[d] );
				Assert.assertEquals( bckVal, bckLong[d] );
				
				Assert.assertEquals( fwdVal, raFwd.getIntPosition( d ) );
				Assert.assertEquals( bckVal, raBck.getIntPosition( d ) );
				
			}
			
		}
		
	}

}
