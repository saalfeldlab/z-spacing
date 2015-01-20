package org.janelia.utility.arrays;

import java.util.Arrays;
import java.util.TreeMap;


public class ArraySortedIndices {
	
	public static int[] sort( final double[] input ) {
		final int[] indices = new int[ input.length ];
		final TreeMap<Double, Integer> tm = sortedKeysAndValues( input );
		int index = 0;
		for ( final Integer v : tm.values() ) {
			indices[index] = v;
			++index;
		}
		return indices;
	}
	
	public static int[] getSortedIndicesFromMap( final TreeMap< Double, Integer > tm ) {
		final int[] result = new int[ tm.size() ];
		int index = 0;
		for ( final Integer v : tm.values() ) {
			result[index++] = v;
		}
		return result;
	}
	
	public static double[] getSortedArrayFromMap( final TreeMap< Double, Integer > tm ) {
		final double[] result = new double[ tm.size() ];
		int index = 0;
		for ( final Double v : tm.keySet() ) {
			result[index++] = v;
		}
		return result;
	}
	
	public static TreeMap< Double, Integer > sortedKeysAndValues( final double[] input ) {
		final TreeMap<Double, Integer> tm = new TreeMap< Double, Integer >();
		for (int i = 0; i < input.length; i++) {
			tm.put( input[i], i );
		}
		return tm;
	}

	public static void main(final String[] args) {
		
		final double[] a = new double[] { 1.0, 2.0, 3.0, 4.0, 2.5 };
		final int[] indices = sort ( a );
		System.out.println( Arrays.toString( indices ) );
		final TreeMap<Double, Integer> tm = sortedKeysAndValues( a );
		System.out.println( Arrays.toString( getSortedIndicesFromMap( tm ) ) );
		System.out.println( Arrays.toString( getSortedArrayFromMap( tm ) ) );
		final double[] b = a.clone();
		for ( int i = 0; i < b.length; ++i ) {
			b[indices[i]] = a[i];
//			b[i] = a[indices[i]];
		}
		System.out.println( Arrays.toString(b) );
	}

}

