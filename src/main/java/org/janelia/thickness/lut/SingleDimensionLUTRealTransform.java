/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
/**
 * 
 */
package org.janelia.thickness.lut;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Threadsafe.
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * 
 */
public class SingleDimensionLUTRealTransform extends AbstractLUTRealTransform
{

	final protected int d;

	public SingleDimensionLUTRealTransform( final double[] lut, final int numSourceDimensions, final int numTargetDimensions, final int d )
	{
		super( lut, numSourceDimensions, numTargetDimensions );
		this.d = d;
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( @SuppressWarnings( "hiding" )
		int d = 0; d < target.length; d++ )
			target[ d ] = source[ d ];

		target[ d ] = applyChecked( source[ d ] );
	}

	@Override
	public void apply( final float[] source, final float[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( @SuppressWarnings( "hiding" )
		int d = 0; d < target.length; d++ )
			target[ d ] = source[ d ];

		target[ d ] = ( float ) applyChecked( source[ d ] );
	}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		assert source.numDimensions() == target.numDimensions(): "Dimensions do not match.";

		target.setPosition( source );

		target.setPosition( applyChecked( source.getDoublePosition( d ) ), d );
	}

	/**
	 * Reuses the LUT
	 */
	@Override
	public SingleDimensionLUTRealTransform copy()
	{
		return new SingleDimensionLUTRealTransform( lut, numSourceDimensions, numTargetDimensions, d );
	}

	@Override
	public void applyInverse( final double[] source, final double[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( @SuppressWarnings( "hiding" )
		int d = 0; d < target.length; d++ )
			source[ d ] = target[ d ];

		source[ d ] = applyInverseChecked( target[ d ] );
	}

	@Override
	public void applyInverse( final float[] source, final float[] target )
	{
		assert source.length == target.length: "Dimensions do not match.";

		for ( @SuppressWarnings( "hiding" )
		int d = 0; d < target.length; d++ )
			source[ d ] = target[ d ];

		source[ d ] = ( float ) applyInverseChecked( target[ d ] );
	}

	@Override
	public void applyInverse( final RealPositionable source, final RealLocalizable target )
	{
		assert source.numDimensions() == target.numDimensions(): "Dimensions do not match.";

		source.setPosition( target );

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

	final static public void main( final String[] args )
	{
		new ImageJ();
		final ImagePlus imp = new ImagePlus( "http://media.npr.org/images/picture-show-flickr-promo.jpg" );
		imp.show();

		final float[] pixels = ( float[] ) imp.getProcessor().convertToFloat().getPixels();
		final ArrayImg< FloatType, FloatArray > img = ArrayImgs.floats( pixels, imp.getWidth(), imp.getHeight() );

		final double[] lut = new double[ Math.max( imp.getWidth(), imp.getHeight() ) ];
		for ( int i = 0; i < lut.length; ++i )
			lut[ i ] = i + Math.pow( i, 1.5 );

		final SingleDimensionLUTRealTransform transform = new SingleDimensionLUTRealTransform( lut, 2, 2, 1 );
		final RealRandomAccessible< FloatType > source = Views.interpolate( Views.extendBorder( img ), new NLinearInterpolatorFactory< FloatType >() );
		final RandomAccessible< FloatType > target = new RealTransformRandomAccessible< FloatType, RealTransform >( source, transform );
		final RandomAccessible< FloatType > target2 = RealViews.transform( source, transform );

//		RealViews.transformReal(source, transform);

		ImageJFunctions.show( Views.interval( target, new FinalInterval( imp.getWidth(), imp.getHeight() ) ) );
		ImageJFunctions.show( Views.interval( target2, new FinalInterval( imp.getWidth(), imp.getHeight() ) ) );
	}
}
