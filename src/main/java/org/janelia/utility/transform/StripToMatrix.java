/**
 * 
 */
package org.janelia.utility.transform;

import ij.ImageJ;

import java.util.Random;

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

import org.janelia.utility.CopyFromIntervalToInterval;

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
	
	public static void main(final String[] args) {
		final Random rng = new Random( 100 );
		final ArrayImg<DoubleType, DoubleArray> strip = ArrayImgs.doubles( 51, 100 );
		final double[] muls = new double[ 100 ];
		for (int i = 0; i < muls.length; i++) {
			muls[i] = rng.nextDouble() / 10 + 1;
		}
		for ( final ArrayCursor<DoubleType> c = strip.cursor(); c.hasNext(); ) {
			c.fwd();
			final double val = Math.exp( - ( c.getDoublePosition( 0 ) - 25 ) * ( c.getDoublePosition( 0 ) - 25 ) / 400.0 );
			c.get().set( val * muls[ c.getIntPosition( 1 ) ] );
		}
		final StripToMatrix tf = new StripToMatrix( 25 );
		final IntervalView<DoubleType> subStrip = Views.interval( strip, new long[] { 0, 10 }, new long[] { 50, 99 } );
		final ArrayImg<DoubleType, DoubleArray> store = ArrayImgs.doubles( 51, 90 );
		CopyFromIntervalToInterval.copyToRealType( subStrip, store );
		final TransformView<DoubleType> matrix = new TransformView< DoubleType >( Views.extendValue( store, new DoubleType( Double.NaN ) ), tf );
		final IntervalView<DoubleType> matrixInterval = Views.interval( matrix, new FinalInterval( 90, 90 ) );
		final IntervalView<DoubleType> backStrip = Views.interval( new TransformView< DoubleType >( Views.extendValue( matrixInterval, new DoubleType( Double.NaN ) ), new MatrixToStrip( 50 ) ), subStrip );
		new ImageJ();
		ImageJFunctions.show( strip, "strip" );
		ImageJFunctions.show( subStrip, "subStrip" );
		ImageJFunctions.show( matrixInterval, "matrix" );
		ImageJFunctions.show( backStrip, "backStrip" );
	}
	
}
