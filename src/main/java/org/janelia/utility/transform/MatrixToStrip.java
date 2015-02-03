package org.janelia.utility.transform;

import ij.ImageJ;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.transform.InvertibleTransform;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

public class MatrixToStrip implements InvertibleTransform {

	private final int numSourceDimensions = 2;
	private final int numTargetDimensions = 2;
	private final int range;
	private final StripToMatrix inverse;
	private final int targetWidth;

	/**
	 * @param range
	 */
	public MatrixToStrip(final int range) {
		this.range = range;
		this.inverse = new StripToMatrix( range, this );
		this.targetWidth = 2*range + 1;
	}

	/**
	 * @param range
	 * @param inverse
	 */
	public MatrixToStrip(final int range, final StripToMatrix inverse) {
		super();
		this.range = range;
		this.inverse = inverse;
		this.targetWidth = 2*range + 1;
	}
	
	public int getTargetWidth() {
		return this.targetWidth;
	}

	@Override
	public int numSourceDimensions() {
		return numSourceDimensions;
	}

	@Override
	public int numTargetDimensions() {
		return numTargetDimensions;
	}

	@Override
	public void apply(final long[] source, final long[] target) {
		final long y      = source[ 1 ];
		final long x      = source[ 0 ] + y - range;
		target[ 0 ] = x;
		target[ 1 ] = y;
	}

	@Override
	public void apply(final int[] source, final int[] target) {
		final int y       = source[ 1 ];
		final int x       = source[ 0 ] + y - range;
		target[ 0 ] = x;
		target[ 1 ] = y;
	}

	@Override
	public void apply(final Localizable source, final Positionable target) {
		final long y = source.getLongPosition( 1 );
		final long x = y + ( source.getLongPosition( 0 ) - range );
		target.setPosition( x, 0 );
		target.setPosition( y, 1 );
	}

	@Override
	public void applyInverse(final long[] source, final long[] target) {
		this.inverse.apply( target, source );
	}

	@Override
	public void applyInverse(final int[] source, final int[] target) {
		this.inverse.apply( target, source );
	}

	@Override
	public void applyInverse(final Positionable source, final Localizable target) {
		this.inverse.apply( target, source );
	}

	@Override
	public InvertibleTransform inverse() {
		return this.inverse;
	}
	
	public static void main(final String[] args) {
		final int rrange = 3;
		final MatrixToStrip t = new MatrixToStrip(rrange);
		final int width = 8;
		final ArrayImg<DoubleType, DoubleArray> mat = ArrayImgs.doubles( width, width );
		for ( final ArrayCursor<DoubleType> c = mat.cursor(); c.hasNext(); ) {
			final DoubleType v = c.next();
			final long x = c.getLongPosition( 0 );
			final long y = c.getLongPosition( 1 );
			final long diff = Math.abs( x - y );
			if ( diff <= rrange )
				v.set( Math.exp( -diff*diff / 100.0 ) );
			else
				v.set( Double.NaN );
		}
		
		new ImageJ();
		ImageJFunctions.show( mat , "mat1" );
		
		final IntervalView<DoubleType> strip = Views.interval( 
				new TransformView<DoubleType>( Views.extendValue( mat, new DoubleType( Double.NaN ) ), t ), 
				new FinalInterval( t.getTargetWidth(), width ) );
		ImageJFunctions.show( strip, "strip" );
		final IntervalView<DoubleType> mat2 = Views.interval( new TransformView< DoubleType >( Views.extendValue( strip, new DoubleType( Double.NaN ) ), t.inverse() ), mat );
		ImageJFunctions.show( mat2, "mat2" );
	}


}
