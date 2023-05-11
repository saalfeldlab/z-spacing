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
/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.Localizable;
import net.imglib2.Positionable;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 *         Apply bijective permutation to one coordinate axis only.
 *
 */
public class SingleDimensionPermutationTransform extends
		AbstractPerumtationTransform
{

	final protected int numSourceDimensions;

	final protected int numTargetDimensions;

	final protected int d;

	/**
	 * @param lut
	 * @param numSourceDimensions
	 *            dimensionality of source
	 * @param numTargetDimensions
	 *            dimensionality of target
	 * @param d
	 *            dimension which shall be transformed. Must be smaller than
	 *            {@link #numSourceDimensions} and {@link #numTargetDimensions}
	 */
	public SingleDimensionPermutationTransform(
			final int[] lut,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int d )
	{
		super( lut );
		assert d > 0 && d < numTargetDimensions && d < numSourceDimensions;
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.d = d;
	}

	@Override
	public void applyInverse( final long[] source, final long[] target )
	{
		System.arraycopy( target, 0, source, 0, this.numSourceDimensions );
		source[ d ] = applyInverse( ( int ) target[ d ] );
	}

	@Override
	public void applyInverse( final int[] source, final int[] target )
	{
		System.arraycopy( target, 0, source, 0, this.numSourceDimensions );
		source[ d ] = applyInverse( target[ d ] );
	}

	@Override
	public void applyInverse( final Positionable source, final Localizable target )
	{
		source.setPosition( target );
		source.setPosition( applyInverse( target.getIntPosition( d ) ), d );
	}

	@Override
	public SingleDimensionPermutationTransform inverse()
	{
		return new SingleDimensionPermutationTransform( inverseLut, numSourceDimensions, numTargetDimensions, d );
	}

	@Override
	public int numSourceDimensions()
	{
		return this.numSourceDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return this.numTargetDimensions;
	}

	@Override
	public void apply( final long[] source, final long[] target )
	{
		System.arraycopy( source, 0, target, 0, this.numTargetDimensions );
		target[ d ] = apply( ( int ) source[ d ] );
	}

	@Override
	public void apply( final int[] source, final int[] target )
	{
		System.arraycopy( source, 0, target, 0, this.numTargetDimensions );
		target[ d ] = apply( source[ d ] );
	}

	@Override
	public void apply( final Localizable source, final Positionable target )
	{
		target.setPosition( source );
		target.setPosition( apply( source.getIntPosition( d ) ), d );
	}

}
