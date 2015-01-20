/**
 * 
 */
package org.janelia.utility.realtransform;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class MatrixToStrip  {
	
	public static < R extends InvertibleRealTransform, T extends RealType< T > & NativeType<T> > 
	RandomAccessibleInterval<T> toStrip( 
			final RandomAccessibleInterval< T > input, 
			final R transform,
			final int range ) {
		final T type = input.randomAccess().get().copy();
		type.setReal( Double.NaN );
		return toStrip(input, transform, range, type);
	}
	
	public static < R extends InvertibleRealTransform, T extends RealType< T > & NativeType<T> > 
	RandomAccessibleInterval<T> toStrip( 
			final RandomAccessibleInterval< T > input, 
			final R transform,
			final int range,
			final T type ) {
		final int width   = 2*range + 1;
		final long height = input.dimension( 0 );
		final long[] dim = new long[] { width, height };
		final ArrayImg<T, ?> result = new ArrayImgFactory< T >().create(dim, type );
		
		final IntervalView<T> extendedByOne = Views.interval( Views.extendBorder( input ), new FinalInterval( input.dimension( 0 ) + 1, input.dimension( 1 ) + 1 ) );
		final RealRandomAccessible< T > source = Views.interpolate( Views.extendValue( extendedByOne, type ), new NLinearInterpolatorFactory< T >());
		final RealTransformRealRandomAccessible<T, InverseRealTransform> source2 = RealViews.transformReal( source, transform );

		final RealRandomAccess< T > access  = source2.realRandomAccess();
		
		for ( int y = 0; y < height; ++y ) {
			access.setPosition( y, 0 );
			access.setPosition( y, 1 );
			transform.apply( access, access );
			final double transformedPosition = access.getDoublePosition( 0 );
			access.setPosition( transformedPosition - range, 0 );
			final Cursor<T> target = Views.flatIterable( Views.hyperSlice( result, 1, y ) ).cursor();
			while( target.hasNext() ) {
				target.next().setReal( access.get().getRealDouble() );
				access.fwd( 0 );
			}
			
		}
		
		return result;
	}
			
	
	
}
