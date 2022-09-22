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
public class LUTRealTransformField extends AbstractLUTRealTransformField
{

	public LUTRealTransformField(
			final int numSourceDimensions,
			final int numTargetDimensions,
			final RandomAccessibleInterval< DoubleType > luts )
	{
		super( numSourceDimensions, numTargetDimensions, luts );
	}

	public LUTRealTransformField(
			final double[] lut,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final long[] dimensions )
	{
		super( lut, numSourceDimensions, numTargetDimensions, dimensions );
	}

	@Override
	public void apply( final double[] source, final double[] target )
	{
		target[ 0 ] = source[ 0 ];
		target[ 1 ] = source[ 1 ];
		for ( int d = 2; d < source.length; ++d )
		{
			target[ d ] = applyChecked( source[ d ], ( int ) source[ 0 ], ( int ) source[ 1 ] );
		}
	}

	@Override
	public void apply( final float[] source, final float[] target )
	{
		target[ 0 ] = source[ 0 ];
		target[ 1 ] = source[ 1 ];
		for ( int d = 2; d < source.length; ++d )
		{
			target[ d ] = ( float ) applyChecked( source[ d ], ( int ) source[ 0 ], ( int ) source[ 1 ] );
		}
	}

	@Override
	public void apply( final RealLocalizable source, final RealPositionable target )
	{
		target.setPosition( source );
		for ( int d = 2; d < source.numDimensions(); ++d )
		{
			target.setPosition( applyChecked( source.getDoublePosition( d ), ( int ) source.getDoublePosition( 0 ), ( int ) source.getDoublePosition( 1 ) ), d );
		}
	}

	@Override
	public void applyInverse( final double[] source, final double[] target )
	{
		source[ 0 ] = target[ 0 ];
		source[ 1 ] = target[ 1 ];
		for ( int d = 2; d < target.length; ++d )
		{
			source[ d ] = applyInverseChecked( target[ d ], ( int ) target[ 0 ], ( int ) target[ 1 ] );
		}
	}

	@Override
	public void applyInverse( final float[] source, final float[] target )
	{
		source[ 0 ] = target[ 0 ];
		source[ 1 ] = target[ 1 ];
		for ( int d = 2; d < target.length; ++d )
		{
			source[ d ] = ( float ) applyInverseChecked( target[ d ], ( int ) target[ 0 ], ( int ) target[ 1 ] );
		}
	}

	@Override
	public void applyInverse( final RealPositionable source, final RealLocalizable target )
	{
		source.setPosition( target );
		for ( int d = 2; d < target.numDimensions(); ++d )
		{
			source.setPosition( applyInverseChecked( target.getDoublePosition( d ), ( int ) target.getDoublePosition( 0 ), ( int ) target.getDoublePosition( 1 ) ), d );
		}
	}

	@Override
	public InvertibleRealTransform inverse()
	{
		return new InverseRealTransform( this );
	}

	@Override
	public InvertibleRealTransform copy()
	{
		return new LUTRealTransformField( numSourceDimensions, numTargetDimensions, luts );
	}

}
