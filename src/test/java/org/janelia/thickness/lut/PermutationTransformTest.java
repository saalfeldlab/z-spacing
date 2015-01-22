/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.thickness.lut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 */
public class PermutationTransformTest
{
	final static Random rnd = new Random();
	static long[] values;
	static int[] lut;
	static final int width = 5;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		values = new long[width * width];
		for ( int i = 0; i < values.length; ++i )
			values[ i ] = rnd.nextLong();

		lut = new int[ width ];

		final ArrayList< Integer > indices = new ArrayList< Integer >();
		for ( int i = 0; i < width; ++i )
			indices.add( i );

		Collections.shuffle( indices );

		for ( int i = 0; i < width; ++i )
			lut[ i ] = indices.get( i );
	}

	@Test
	public void test()
	{
		final ArrayImg< LongType, LongArray > img = ArrayImgs.longs( values, width, width );
		final PermutationTransform t = new PermutationTransform( lut, 2, 2 );
		final TransformView< LongType > bijectivePermutation = new TransformView< LongType >( img, t );
		final TransformView< LongType > inverseBijectivePermutation = new TransformView< LongType >( bijectivePermutation, t.inverse() );
		System.out.println( Arrays.toString( lut ) );

		final RandomAccess< LongType > a = img.randomAccess();
		final RandomAccess< LongType > b = new IterableRandomAccessibleInterval< LongType >( Views.interval( inverseBijectivePermutation, img ) ).randomAccess();

		for ( int i = 0; i < 1000; ++i )
		{
			final long[] x = new long[]{ rnd.nextInt( width ), rnd.nextInt( width ) };
			a.setPosition( x );
			b.setPosition( x );
			Assert.assertEquals( a.get().get(), b.get().get() );
		}

		int i = 0;
//		for ( final LongType l : new IterableRandomAccessibleInterval< LongType >( Views.interval( inverseBijectivePermutation, img ) ) )
		for ( final LongType l : Views.flatIterable( Views.interval( inverseBijectivePermutation, img ) ) )
		{

				Assert.assertEquals( values[ i++ ], l.get() );
		}
	}
}
