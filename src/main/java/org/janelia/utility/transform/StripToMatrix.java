/**
 * 
 */
package org.janelia.utility.transform;

import java.util.Arrays;

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

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class StripToMatrix implements InvertibleTransform {
	
	private final int numSourceDimensions = 2;
	private final int numTargetDimensions = 2;
	private final int range;
	private final MatrixToStrip inverse;

	/**
	 * @param range
	 */
	public StripToMatrix(final int range) {
		this.range       = range;
		this.inverse     = new MatrixToStrip( range, this );
	}
	
	public StripToMatrix( final int range, final MatrixToStrip inverse ) {
		this.range       = range;
		this.inverse     = inverse;
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
		final long x      = source[ 0 ] - y + range;
		target[ 0 ] = x;
		target[ 1 ] = y;
	}

	@Override
	public void apply(final int[] source, final int[] target) {
		final int y       = source[ 1 ];
		final int x       = source[ 0 ] - y + range;
		target[ 0 ] = x;
		target[ 1 ] = y;
	}

	@Override
	public void apply(final Localizable source, final Positionable target) {
		final long y = source.getLongPosition( 1 );
		final long x = source.getLongPosition( 0 ) - y + range;
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
	
}
