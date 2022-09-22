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
package org.janelia.utility.arrays;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ArraySortedIndices
{

	public static int[] sort( final double[] input )
	{
		final int[] indices = new int[ input.length ];
		final TreeMap< Double, Integer > tm = sortedKeysAndValues( input );
		int index = 0;
		for ( final Integer v : tm.values() )
		{
			indices[ index ] = v;
			++index;
		}
		return indices;
	}

	public static int[] sortForward( final double[] input )
	{
		final int[] arr = sort( input );
		final int[] res = new int[ arr.length ];
		for ( int i = 0; i < arr.length; i++ )
		{
			res[ arr[ i ] ] = i;
		}
		return res;
	}

	public static void sort( final double[] input, final int[] forward, final int[] backward )
	{
		final TreeMap< Double, Integer > tm = sortedKeysAndValues( input );
		int index = 0;
		for ( final Entry< Double, Integer > entry : tm.entrySet() )
		{
			backward[ index ] = entry.getValue();
			forward[ backward[ index ] ] = index;
			input[ index ] = entry.getKey();
			++index;
		}
	}

	public static int[] getSortedIndicesFromMap( final TreeMap< Double, Integer > tm )
	{
		final int[] result = new int[ tm.size() ];
		int index = 0;
		for ( final Integer v : tm.values() )
		{
			result[ index++ ] = v;
		}
		return result;
	}

	public static double[] getSortedArrayFromMap( final TreeMap< Double, Integer > tm )
	{
		final double[] result = new double[ tm.size() ];
		int index = 0;
		for ( final Double v : tm.keySet() )
		{
			result[ index++ ] = v;
		}
		return result;
	}

	public static TreeMap< Double, Integer > sortedKeysAndValues( final double[] input )
	{
		final TreeMap< Double, Integer > tm = new TreeMap< Double, Integer >();
		for ( int i = 0; i < input.length; i++ )
		{
			tm.put( input[ i ], i );
		}
		return tm;
	}

	public static void main( final String[] args )
	{

		final double[] a = new double[] { 1.0, 2.0, 3.0, 4.0, 2.5 };
		final double[] c = a.clone();
		final int[] indices = sort( a );
		System.out.println( Arrays.toString( indices ) );
		final TreeMap< Double, Integer > tm = sortedKeysAndValues( a );
		System.out.println( Arrays.toString( getSortedIndicesFromMap( tm ) ) );
		System.out.println( Arrays.toString( getSortedArrayFromMap( tm ) ) );
		final double[] b = a.clone();
		for ( int i = 0; i < b.length; ++i )
		{
			b[ indices[ i ] ] = a[ i ];
//			b[i] = a[indices[i]];
		}
		System.out.println( Arrays.toString( b ) );

		final int[] fwd = new int[ c.length ];
		final int[] bck = new int[ c.length ];
		sort( c, fwd, bck );
		System.out.println();
		System.out.println( Arrays.toString( a ) );
		System.out.println( Arrays.toString( c ) );
		System.out.println( Arrays.toString( fwd ) );
		System.out.println( Arrays.toString( bck ) );
	}

}
