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
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
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
public class LUTRealTransform implements InvertibleRealTransform
{
	final protected int numSourceDimensions;
	final protected int numTargetDimensions;
	final protected int lutMaxIndex;
	final protected double[] lut;
	
	public LUTRealTransform( final double[] lut, final int numSourceDimensions, final int numTargetDimensions )
	{
		this.lut = lut;
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;

		lutMaxIndex = lut.length - 1;
	}
	
	private double apply( final double x )
	{
		final int xFloor = ( int )x;
		final double dx = x - xFloor;
		return ( lut[ xFloor + 1 ] - lut[ xFloor ] ) * dx + lut[ xFloor ];
	}

	private double applyChecked( final double x )
	{
		if ( x < 0 ) return -Double.MAX_VALUE;
		else if ( x >= lutMaxIndex ) return Double.MAX_VALUE;
		else return apply( x );
	}
	
	/**
	 * Finds the LUT index i of the largest value smaller than or equal y for
	 * all y in [lut[0],lut[max]] both inclusive.  Only exception is lut[max]
	 * for which it returns max-1.  This is the correct behavior for
	 * interpolating between lut[i] and lut[i + i] including lut[max].
	 * 
	 * Implemented as bin-search.
	 * 
	 * @param y
	 * @return
	 */
	private int findFloorIndex( final double y )
	{
		int min = 0;
		int max = lutMaxIndex;
		int i = max >> 1;
		do
		{
			if ( lut[ i ] > y )
				max = i;
			else
				min = i;
			i = ( ( max - min ) >> 1 ) + min;
		}
		while ( i != min );
		return i;

	}
	
	private double applyInverse( final double y )
	{
		final int i = findFloorIndex( y );
		
		final double x1 = lut[ i ];
		final double x2 = lut[ i + 1 ];
		
		return ( y - x1 )  / ( x2 - x1 ) + i;
	}
	
	private double applyInverseChecked( final double y )
	{
		if ( y < lut[ 0 ] )
			return -Double.MAX_VALUE;
		else if ( y > lut[ lutMaxIndex ] )
			return Double.MAX_VALUE;
		else
			return applyInverse( y );
	}
	
	public double minTransformedCoordinate()
	{
		return lut[ 0 ];
	}

	public double maxTransformedCoordinate()
	{
		return lut[ lutMaxIndex ];
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( int d = 0; d < source.length; ++d )
			target[ d ] = applyChecked( source[ d ] );
	}

	@Override
	public void apply( final float[] source, final float[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( int d = 0; d < source.length; ++d )
			target[ d ] = ( float ) applyChecked( source[ d ] );
	}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		assert source.numDimensions() == target.numDimensions(): "Dimensions do not match.";

		final int n = source.numDimensions();
		for ( int d = 0; d < n; ++d )
			target.setPosition( applyChecked( source.getDoublePosition( d ) ), d );
	}

	/**
	 * Reuses the LUT.
	 */
	@Override
	public LUTRealTransform copy()
	{
		return new LUTRealTransform( lut, numSourceDimensions, numTargetDimensions );
	}

	@Override
	public int numSourceDimensions()
	{
		return numSourceDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return numTargetDimensions;
	}
	
	public static < T extends Type< T >> void render( final RealRandomAccessible< T > source, final RandomAccessibleInterval< T > target, final RealTransform transform, final double dx )
	{
		final RealRandomAccessible< T > interpolant = Views.interpolate( Views.extendBorder( target ), new NearestNeighborInterpolatorFactory< T >() );
		final RealRandomAccess< T > a = source.realRandomAccess();
		final RealRandomAccess< T > b = interpolant.realRandomAccess();

		for ( double y = 0; y < target.dimension( 1 ); y += dx )
		{
			a.setPosition( y, 1 );

			for ( double x = 0; x < target.dimension( 0 ); x += dx )
			{
				a.setPosition( x, 0 );
				transform.apply( a, b );
				b.get().set( a.get() );
			}
		}
	}
	
	
	@Override
	public void applyInverse( final double[] source, final double[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( int d = 0; d < target.length; ++d )
			source[ d ] = applyInverseChecked( target[ d ] );
	}

	@Override
	public void applyInverse( final float[] source, final float[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( int d = 0; d < target.length; ++d )
			source[ d ] = ( float ) applyInverseChecked( target[ d ] );
	}

	@Override
	public void applyInverse( final RealPositionable source, final RealLocalizable target )
	{
		assert source.numDimensions() == target.numDimensions(): "Dimensions do not match.";
		
		final int n = target.numDimensions();
		for ( int d = 0; d < n; ++d )
			source.setPosition( applyInverseChecked( target.getDoublePosition( d ) ), d );
	}
	
	/** 
	 * TODO create actual inverse
	 */
	@Override
	public InvertibleRealTransform inverse()
	{
		return new InverseRealTransform( this );
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
		final RandomAccessible<FloatType> target2 = RealViews.transform(source, transform);
		
//		RealViews.transformReal(source, transform);
		
		final ArrayImg<FloatType, FloatArray> targetImg = ArrayImgs.floats(imp.getWidth(), imp.getHeight());
		render(source, targetImg, transform, 0.05);
		
		ImageJFunctions.show(Views.interval(target, new FinalInterval(imp.getWidth(), imp.getHeight())));
		ImageJFunctions.show(Views.interval(target2, new FinalInterval(imp.getWidth(), imp.getHeight())));
		ImageJFunctions.show(targetImg);
	}
}
