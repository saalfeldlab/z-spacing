/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2023 Howard Hughes Medical Institute.
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
package org.janelia.thickness.lut;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class SingleDimensionLUTGrid extends AbstractLUTGrid
{

	private final int applyTransformToDimension;

	public SingleDimensionLUTGrid(
			final int numSourceDimensions,
			final int numTargetDimensions,
			final RandomAccessibleInterval< DoubleType > lutArray,
			final int applyTransformToDimension )
	{
		this( numSourceDimensions, numTargetDimensions, lutArray, applyTransformToDimension, new double[] { 1.0 }, new double[] { 0.0 } );
	}

	public SingleDimensionLUTGrid(
			final int numSourceDimensions,
			final int numTargetDimensions,
			final RandomAccessibleInterval< DoubleType > lutArray,
			final int applyTransformToDimension,
			final double[] scale,
			final double[] shift )
	{
		super( numSourceDimensions, numTargetDimensions, lutArray, scale, shift );
		this.applyTransformToDimension = applyTransformToDimension;
		assert this.applyTransformToDimension >= this.nNonTransformedCoordinates;
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			target[ d ] = source[ d ];
		}
		target[ this.applyTransformToDimension ] = this.applyChecked( source[ this.applyTransformToDimension ] );
	}

	@Override
	public void apply( final float[] source, final float[] target )
	{
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			target[ d ] = source[ d ];
		}
		target[ applyTransformToDimension ] = ( float ) this.applyChecked( source[ applyTransformToDimension ] );
	}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			target.setPosition( source.getDoublePosition( d ), d );
		}
		target.setPosition( this.apply( source.getDoublePosition( applyTransformToDimension ) ),
				applyTransformToDimension );
	}

	@Override
	public void applyInverse( final double[] source, final double[] target )
	{
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			source[ d ] = target[ d ];
		}
		source[ applyTransformToDimension ] = this.applyInverseChecked( target[ applyTransformToDimension ] );
	}

	@Override
	public void applyInverse( final float[] source, final float[] target )
	{
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			source[ d ] = target[ d ];
		}
		source[ applyTransformToDimension ] = ( float ) this.applyInverseChecked( target[ applyTransformToDimension ] );
	}

	@Override
	public void applyInverse( final RealPositionable source, final RealLocalizable target )
	{
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			source.setPosition( target.getDoublePosition( d ), d );
		}
		source.setPosition( this.applyInverseChecked( target.getDoublePosition( applyTransformToDimension ) ),
				applyTransformToDimension );
	}

	@Override
	public InvertibleRealTransform inverse()
	{
		return new InverseRealTransform( this );
	}

	@Override
	public InvertibleRealTransform copy()
	{
		return new SingleDimensionLUTGrid( applyTransformToDimension,
				applyTransformToDimension,
				lutArray,
				applyTransformToDimension,
				scale,
				shift );
	}

	public SingleDimensionLUTGrid reScale( final double... scale )
	{
		final double[] sc = new double[ this.scale.length ];
		for ( int i = 0; i < scale.length; i++ )
		{
			sc[ i ] = scale[ i ];
		}
		for ( int i = scale.length; i < sc.length; ++i )
		{
			sc[ i ] = scale[ scale.length - 1 ];
		}
		return new SingleDimensionLUTGrid( applyTransformToDimension, applyTransformToDimension, lutArray, applyTransformToDimension, sc, shift );
	}

	public SingleDimensionLUTGrid reShift( final double... shift )
	{
		final double[] sh = new double[ this.shift.length ];
		for ( int i = 0; i < shift.length; i++ )
		{
			sh[ i ] = shift[ i ];
		}
		for ( int i = shift.length; i < sh.length; ++i )
		{
			sh[ i ] = shift[ shift.length - 1 ];
		}
		return new SingleDimensionLUTGrid( applyTransformToDimension, applyTransformToDimension, lutArray, applyTransformToDimension, scale, sh );
	}

}
