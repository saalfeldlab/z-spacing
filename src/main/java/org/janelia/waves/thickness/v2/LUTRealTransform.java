/**
 * 
 */
package org.janelia.waves.thickness.v2;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * This is not thread-safe. lut array can be re-used across multiple instances
 * but instance should not.
 * 
 * @author hanslovskyp
 * @author saalfelds
 * 
 */
public class LUTRealTransform implements RealTransform {

	final protected int numSourceDimensions;
	final protected int numTargetDimensions;
	final protected RealRandomAccess<DoubleType> access;
	final protected int lutMaxIndex;
	
	private LUTRealTransform(final RealRandomAccess<DoubleType> access,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int lutMaxIndex ) {
		this.access = access.copyRealRandomAccess();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.lutMaxIndex = lutMaxIndex;
	}

	public LUTRealTransform(
			final double[] lut,
			final InterpolatorFactory<DoubleType, RandomAccessible<DoubleType>> interpolatorFactory,
			final int numSourceDimensions,
			final int numTargetDimensions ) {
		access = Views.interpolate(
				Views.extendBorder(ArrayImgs.doubles(lut, lut.length)),
				interpolatorFactory).realRandomAccess();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.lutMaxIndex = lut.length - 1;
	}

	public LUTRealTransform(
			final double[] lut,
			final int numSourceDimensions,
			final int numTargetDimensions ) {
		this(lut, new NLinearInterpolatorFactory<DoubleType>(), numSourceDimensions, numTargetDimensions);
	}

	public void apply(double[] arg0, double[] arg1) {
		assert arg0.length == arg1.length : "Dimensions do not match.";

		for (int d = 0; d < arg0.length; ++d) {
			if (arg0[d] < 0)
				arg1[d] = -Double.MAX_VALUE;
			else if (arg0[d] > lutMaxIndex )
				arg1[d] = Double.MAX_VALUE;
			else {
				access.setPosition(arg0[d], 0);
				arg1[d] = access.get().getRealDouble();
			}
		}
	}

	public void apply(float[] arg0, float[] arg1) {
		assert arg0.length == arg1.length : "Dimensions do not match.";

		for (int d = 0; d < arg0.length; ++d) {
			if (arg0[d] < 0)
				arg1[d] = -Float.MAX_VALUE;
			else if (arg0[d] > lutMaxIndex )
				arg1[d] = Float.MAX_VALUE;
			else {
				access.setPosition(arg0[d], 0);
				arg1[d] = access.get().getRealFloat();
			}
		}
	}

	public void apply(RealLocalizable arg0, RealPositionable arg1) {
		assert arg0.numDimensions() == arg1.numDimensions() : "Dimensions do not match.";
		
		final int n = arg0.numDimensions();
		for (int d = 0; d < n; ++d) {
			final double arg0d = arg0.getDoublePosition(d);
			if (arg0d < 0)
				arg1.setPosition( -Double.MAX_VALUE, d);
			else if (arg0d > lutMaxIndex )
				arg1.setPosition( Double.MAX_VALUE, d );
			else {
				access.setPosition(arg0d, 0);
				arg1.setPosition(access.get().getRealDouble(), d);
			}
		}
	}

	/**
	 * Reuses the RealRandomAccessible, generates a new RealRandomAccess.
	 */
	public RealTransform copy() {
		return new LUTRealTransform(access, numSourceDimensions, numTargetDimensions, lutMaxIndex );
	}

	public int numSourceDimensions() {
		return numSourceDimensions;
	}

	public int numTargetDimensions() {
		return numTargetDimensions;
	}
	
	public static <T extends Type<T>> void render( final RealRandomAccessible< T > source, final RandomAccessibleInterval< T > target, final RealTransform transform, final double dx ) {
		final RealRandomAccessible<T> interpolant = Views.interpolate(Views.extendBorder( target ), new NearestNeighborInterpolatorFactory<T>());
		final RealRandomAccess<T> a = source.realRandomAccess();
		final RealRandomAccess<T> b = interpolant.realRandomAccess();
		
		for ( double y = 0; y < target.dimension(1); y += dx) {
			a.setPosition(y, 1);
			
			for ( double x = 0; x < target.dimension(0); x += dx) {
				a.setPosition(x, 0);
				transform.apply(a, b);
				b.get().set(a.get());
			}
		}
	}
	
	final static public void main( final String[] args ) {
		new ImageJ();
		final ImagePlus imp = new ImagePlus( "http://media.npr.org/images/picture-show-flickr-promo.jpg" );
		imp.show();
		
		final float[] pixels = ( float[] )imp.getProcessor().convertToFloat().getPixels();
		final ArrayImg<FloatType, FloatArray> img = ArrayImgs.floats(pixels, imp.getWidth(), imp.getHeight());
		
		final double[] lut = new double[Math.max(imp.getWidth(), imp.getHeight())];
		for (int i =0; i < lut.length; ++i)
			lut[i] = i + Math.pow(i, 1.5);
		
		final LUTRealTransform transform = new LUTRealTransform(lut, 2, 2);
		final RealRandomAccessible<FloatType> source = Views.interpolate( Views.extendBorder(img), new NLinearInterpolatorFactory<FloatType>() );
		final RandomAccessible<FloatType> target = new RealTransformRandomAccessible<FloatType, RealTransform>(source, transform);
		
//		RealViews.transformReal(source, transform);
		
		final ArrayImg<FloatType, FloatArray> targetImg = ArrayImgs.floats(imp.getWidth(), imp.getHeight());
		render(source, targetImg, transform, 0.05);
		
		ImageJFunctions.show(Views.interval(target, new FinalInterval(imp.getWidth(), imp.getHeight())));
		ImageJFunctions.show(targetImg);
	}
}
