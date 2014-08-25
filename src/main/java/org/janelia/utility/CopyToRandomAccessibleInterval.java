package org.janelia.utility;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public class CopyToRandomAccessibleInterval {
	
	public static < N extends Number, T extends RealType<T> > void copy( ListImg< N[] > source, RandomAccessibleInterval< T > target ) {
		int maxTargetDimension = target.numDimensions() - 1;
		assert source.numDimensions() == maxTargetDimension;
		assert source.firstElement().length == target.dimension( maxTargetDimension );
		for ( int d = 0; d < source.numDimensions(); ++d ) {
			assert source.dimension( d ) == target.dimension( d );
		}
		
		ListCursor<N[]> s = source.cursor();
		RandomAccess<T> t = target.randomAccess();
		
		while ( s.hasNext() ) {
			s.fwd();
			N[] arr = s.get();
			for ( int d = 0; d < maxTargetDimension; ++d ) {
				t.setPosition( s.getLongPosition( d ), d );
			}
			for (int i = 0; i < arr.length; i++) {
				t.setPosition( i, maxTargetDimension );
				t.get().setReal( arr[i].doubleValue() );
			}
		}
		
	}
	
	public static void main(String[] args) {
		Integer[] arr = new Integer[]{ 1, 2, 3 };
		ListImg< Integer[] > img = new ListImg< Integer[] >( new long[] { 1 }, arr );
		ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( 1, arr.length );
		ListCursor<Integer[]> c = img.cursor();
		c.fwd();
		c.set( arr );
		copy( img, result );
		ArrayCursor<DoubleType> r = result.cursor();
		for (int i = 0; i < arr.length; i++) {
			System.out.println( arr[i] + " ~> " + r.next().get() );
		}
	}

}
