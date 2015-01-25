package org.janelia.utility.io.transform;

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

public class MatrixToStrip implements InvertibleTransform {

	private final int numSourceDimensions = 2;
	private final int numTargetDimensions = 2;
	private final int range;
	private final StripToMatrix inverse;
	private final int targetWidth;

	/**
	 * @param range
	 */
	protected MatrixToStrip(int range) {
		this.range = range;
		this.inverse = new StripToMatrix( range, this );
		this.targetWidth = 2*range + 1;
	}

	/**
	 * @param range
	 * @param inverse
	 */
	public MatrixToStrip(int range, StripToMatrix inverse) {
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
	public void apply(long[] source, long[] target) {
		long y      = source[ 1 ];
		long x      = source[ 0 ] + y - range;
		target[ 0 ] = x;
		target[ 1 ] = y;
		System.out.println( Arrays.toString( source ) + " ~> " + Arrays.toString( target ) );
	}

	@Override
	public void apply(int[] source, int[] target) {
		int y       = source[ 1 ];
		int x       = source[ 0 ] + y - range;
		target[ 0 ] = x;
		target[ 1 ] = y;
	}

	@Override
	public void apply(Localizable source, Positionable target) {
		long y = source.getLongPosition( 1 );
		long x = y + ( source.getLongPosition( 0 ) - range );
		target.setPosition( x, 0 );
		target.setPosition( y, 1 );
	}

	@Override
	public void applyInverse(long[] source, long[] target) {
		this.inverse.apply( target, source );
	}

	@Override
	public void applyInverse(int[] source, int[] target) {
		this.inverse.apply( target, source );
	}

	@Override
	public void applyInverse(Positionable source, Localizable target) {
		this.inverse.apply( target, source );
	}

	@Override
	public InvertibleTransform inverse() {
		return this.inverse;
	}
	
	public static void main(String[] args) {
		int rrange = 3;
		MatrixToStrip t = new MatrixToStrip(rrange);
		int width = 8;
		ArrayImg<DoubleType, DoubleArray> mat = ArrayImgs.doubles( width, width );
		for ( ArrayCursor<DoubleType> c = mat.cursor(); c.hasNext(); ) {
			DoubleType v = c.next();
			long x = c.getLongPosition( 0 );
			long y = c.getLongPosition( 1 );
			long diff = Math.abs( x - y );
			if ( diff <= rrange )
				v.set( Math.exp( -diff*diff / 100.0 ) );
			else
				v.set( Double.NaN );
		}
		
		new ImageJ();
		ImageJFunctions.show( mat , "mat1" );
		
		IntervalView<DoubleType> strip = Views.interval( 
				new TransformView<DoubleType>( Views.extendValue( mat, new DoubleType( Double.NaN ) ), t ), 
				new FinalInterval( t.getTargetWidth(), width ) );
		ImageJFunctions.show( strip, "strip" );
		IntervalView<DoubleType> mat2 = Views.interval( new TransformView< DoubleType >( Views.extendValue( strip, new DoubleType( Double.NaN ) ), t.inverse() ), mat );
		ImageJFunctions.show( mat2, "mat2" );
	}


}
