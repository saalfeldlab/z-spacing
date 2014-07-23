/**
 * 
 */
package org.janelia.waves.thickness.v2;

import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.collection.KDTree;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
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
public class SingleDimensionLUTRealTransform implements InvertibleRealTransform {

	final protected int numSourceDimensions;
	final protected int numTargetDimensions;
	final protected RealRandomAccess<DoubleType> access;
	final protected int lutMaxIndex;
	final protected double[] lut;
	protected KNearestNeighborSearchOnKDTree<Double> search;
	final protected RealPoint reference = new RealPoint( 1 );
	final protected int d;
	
	private SingleDimensionLUTRealTransform(final RealRandomAccess<DoubleType> access,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int lutMaxIndex,
			final double[] lut,
			final int d ) {
		this.access = access.copyRealRandomAccess();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.lutMaxIndex = lutMaxIndex;
		this.lut = lut.clone();
		this.d = d;
		search = generateInverseLUTSearch(lut);
	}

	public SingleDimensionLUTRealTransform(
			final double[] lut,
			final InterpolatorFactory<DoubleType, RandomAccessible<DoubleType>> interpolatorFactory,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int d) {
		access = Views.interpolate(
				Views.extendBorder(ArrayImgs.doubles(lut, lut.length)),
				interpolatorFactory).realRandomAccess();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.lutMaxIndex = lut.length - 1;
		this.lut = lut.clone();
		this.d = d;
		search = generateInverseLUTSearch(lut);
	}

	public SingleDimensionLUTRealTransform(
			final double[] lut,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int d) {
		this(lut, new NLinearInterpolatorFactory<DoubleType>(), numSourceDimensions, numTargetDimensions, d);
	}
	
	protected static KNearestNeighborSearchOnKDTree< Double > generateInverseLUTSearch( final double[] lut ) {
		final ArrayList< Double > values =  new ArrayList<Double>();
		for ( int i = 0; i < lut.length; ++i )
			values.add( new Double( i ) );
		
		final ArrayList< RealPoint > coordinates =  new ArrayList< RealPoint >();
		for ( final double a : lut )
			coordinates.add( new RealPoint( new double[]{ a } ) );
		
		return new KNearestNeighborSearchOnKDTree< Double >( new KDTree<Double>( values, coordinates ), 2 );
		
	}
	
	public double maxTransformedCoordinate() {
		return lut[ lutMaxIndex ];
	}
	
	public double minTransformedCoordinate() {
		return lut[ 0 ];
	}
	
	public void update( final double[] coordinates ) {
		assert coordinates.length >= lut.length : "Not enough coordinates.";
		
		System.arraycopy(coordinates, 0, lut, 0, lut.length);
		search = generateInverseLUTSearch(lut);
	}

	public void apply(final double[] source, final double[] target) {
		
		for (int d = 0; d < target.length; d++) {
			target[d] = source[d];
		}
		
		// every d below is member d
		if (source[d] < 0)
			target[d] = -Double.MAX_VALUE;
		else if (source[d] > lutMaxIndex )
			target[d] = Double.MAX_VALUE;
		else {
			access.setPosition(source[d], 0);
			target[d] = access.get().getRealDouble();
		}
	}

	public void apply(final float[] source, final float[] target) {
		
		for (int d = 0; d < target.length; d++) {
			target[d] = source[d];
		}
		
		// every d below is member d
		if (source[d] < 0)
			target[d] = -Float.MAX_VALUE;
		else if (source[d] > lutMaxIndex )
			target[d] = Float.MAX_VALUE;
		else {
			access.setPosition(source[d], 0);
			target[d] = access.get().getRealFloat();
		}
	}

	public void apply(final RealLocalizable source, final RealPositionable target) {
		
		target.setPosition(source);
		
		final double arg0d = source.getDoublePosition(d);
		if (arg0d < 0)
			target.setPosition( -Double.MAX_VALUE, d);
		else if (arg0d > lutMaxIndex )
			target.setPosition( Double.MAX_VALUE, d );
		else {
			access.setPosition(arg0d, 0);
			target.setPosition(access.get().getRealDouble(), d);
		}
	}

	/**
	 * Reuses the RealRandomAccessible, generates a new RealRandomAccess.
	 */
	public SingleDimensionLUTRealTransform copy() {
		return new SingleDimensionLUTRealTransform(access, numSourceDimensions, numTargetDimensions, lutMaxIndex, lut, d );
	}

	public int numSourceDimensions() {
		return numSourceDimensions;
	}

	public int numTargetDimensions() {
		return numTargetDimensions;
	}

	public void applyInverse(final double[] source, final double[] target) {
		
		for (int d = 0; d < target.length; d++) {
			source[d] = target[d];
		}
		
		// every d below is member d
		if ( target[ d ] < lut[ 0 ] )
			source[ d ] = -Double.MAX_VALUE;
		else if ( target[ d ] > lut[ lutMaxIndex ] )
			source[ d ] = Double.MAX_VALUE;
		else {
			reference.setPosition( target[ d ], 0 );
			search.search( reference );
			final double d1 = search.getDistance( 0 );
			final double d2 = search.getDistance( 1 );
			final double v1 = search.getSampler( 0 ).get().doubleValue();
			final double v2 = search.getSampler( 1 ).get().doubleValue();
			final double m = 1.0 / ( d1 + d2 );
			source[ d ] = v1 * d2 * m + v2 * d1 * m;
		}
	}

	public void applyInverse(final float[] source, final float[] target) {
		for (int d = 0; d < target.length; d++) {
			source[d] = target[d];
		}
		
		// every d below is member d
		if ( target[ d ] < lut[ 0 ] )
			source[ d ] = -Float.MAX_VALUE;
		else if ( target[ d ] > lut[ lutMaxIndex ] )
			source[ d ] = Float.MAX_VALUE;
		else {
			reference.setPosition( target[ d ], 0 );
			search.search( reference );
			final double d1 = search.getDistance( 0 );
			final double d2 = search.getDistance( 1 );
			final double v1 = search.getSampler( 0 ).get().doubleValue();
			final double v2 = search.getSampler( 1 ).get().doubleValue();
			final double m = 1.0 / ( d1 + d2 );
			source[ d ] = ( float )( v1 * d2 * m + v2 * d1 * m );
		}
	}

	public void applyInverse(final RealPositionable source, final RealLocalizable target) {
		source.setPosition(target);
		final double tp = target.getDoublePosition( d );
		if ( tp < lut[ 0 ] )
			source.setPosition( -Double.MAX_VALUE, d );
		else if ( tp > lut[ lutMaxIndex ] )
			source.setPosition( Double.MAX_VALUE, d );
		else {
			reference.setPosition( tp, 0 );
			search.search( reference );
			final double d1 = search.getDistance( 0 );
			final double d2 = search.getDistance( 1 );
			
			final double v1 = search.getSampler( 0 ).get().doubleValue();
			final double v2 = search.getSampler( 1 ).get().doubleValue();
			
			final double m = 1.0 / ( d1 + d2 );
			source.setPosition( v1 * d2 * m + v2 * d1 * m, d );
		}
	}

	/** 
	 * TODO create actual inverse
	 */
	public InvertibleRealTransform inverse() {
		return new InverseRealTransform(this);
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
		
		final SingleDimensionLUTRealTransform transform = new SingleDimensionLUTRealTransform(lut, 2, 2, 2);
		final RealRandomAccessible<FloatType> source = Views.interpolate( Views.extendBorder(img), new NLinearInterpolatorFactory<FloatType>() );
		final RandomAccessible<FloatType> target = new RealTransformRandomAccessible<FloatType, RealTransform>(source, transform);
		final RandomAccessible<FloatType> target2 = RealViews.transform(source, transform);
		
//		RealViews.transformReal(source, transform);
		
		
		ImageJFunctions.show(Views.interval(target, new FinalInterval(imp.getWidth(), imp.getHeight())));
		ImageJFunctions.show(Views.interval(target2, new FinalInterval(imp.getWidth(), imp.getHeight())));
	}
}
