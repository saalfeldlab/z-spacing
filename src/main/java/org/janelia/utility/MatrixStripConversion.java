package org.janelia.utility;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.shear.AbstractShearTransform;
import net.imglib2.transform.integer.shear.ShearTransform;
import net.imglib2.type.Type;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class MatrixStripConversion
{
	public static < T extends Type< T > > RandomAccessibleInterval< T >
	stripToMatrix( final RandomAccessibleInterval< T > strip, final T dummy )
	{
//		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( strip, dummy );
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( Views.expandBorder( strip, 1, 1 ), dummy );
		final AbstractShearTransform tf = new ShearTransform( 2, 0, 1 ).inverse();
		final long w = strip.dimension( 0 ) / 2;
		final long h = strip.dimension( 1 );
		final FinalInterval interval = new FinalInterval( new long[] { w, 0 }, new long[] { h + w - 1, h - 1 } );
		final IntervalView< T > transformed = Views.offsetInterval( new TransformView<>( extended, tf ), interval );
		return transformed;
	}

	public static < T extends Type< T > > RandomAccessibleInterval< T >
	matrixToStrip( final RandomAccessibleInterval< T > matrix, final int range, final T dummy )
	{
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( matrix, dummy );
		final AbstractShearTransform tf = new ShearTransform( 2, 0, 1 );
		final long h = matrix.dimension( 1 );
		final FinalInterval interval = new FinalInterval( new long[] { -range, 0 }, new long[] { range, h - 1 } );
		final IntervalView< T > transformed = Views.offsetInterval( new TransformView<>( extended, tf ), interval );
		return transformed;
	}
}
