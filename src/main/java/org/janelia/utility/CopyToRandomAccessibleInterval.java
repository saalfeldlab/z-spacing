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
	
	public static < T extends RealType<T> > void copy( final ListImg< double[] > source, final RandomAccessibleInterval< T > target ) {
		final int maxTargetDimension = target.numDimensions() - 1;
		assert source.numDimensions() == maxTargetDimension;
		assert source.firstElement().length == target.dimension( maxTargetDimension );
		for ( int d = 0; d < source.numDimensions(); ++d ) {
			assert source.dimension( d ) == target.dimension( d );
		}
		
		final ListCursor<double[]> s = source.cursor();
		final RandomAccess<T> t = target.randomAccess();
		
		while ( s.hasNext() ) {
			s.fwd();
			final double[] arr = s.get();
			for ( int d = 0; d < maxTargetDimension; ++d ) {
				t.setPosition( s.getLongPosition( d ), d );
			}
			for (int i = 0; i < arr.length; i++) {
				t.setPosition( i, maxTargetDimension );
				t.get().setReal( arr[i] );
			}
		}
		
	}
	
	
	public static void main(final String[] args) {
		final double[] arr = new double[]{ 1, 2, 3 };
		final ListImg< double[] > img = new ListImg< double[] >( new long[] { 1 }, arr );
		final ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( 1, arr.length );
		final ListCursor<double[]> c = img.cursor();
		c.fwd();
		c.set( arr );
		copy( img, result );
		final ArrayCursor<DoubleType> r = result.cursor();
		for (int i = 0; i < arr.length; i++) {
			System.out.println( arr[i] + " ~> " + r.next().get() );
		}
	}

}
