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

import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.transform.InvertibleTransform;

/**
 * Bijective integer transform mapping between integer coordinates in [0,n-1].
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 */
public class PermutationTransform extends AbstractPerumtationTransform
{
	final protected int numSourceDimensions;

	final protected int numTargetDimensions;

	/**
	 *
	 * @param lut
	 *            must be a bijective permutation over its index set, i.e. for a
	 *            lut of legnth n, the sorted content the array must be
	 *            [0,...,n-1] which is the index set of the lut.
	 * @param numSourceDimensions
	 * @param numTargetDimensions
	 */
	public PermutationTransform( final int[] lut, final int numSourceDimensions, final int numTargetDimensions )
	{
		super( lut );
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;

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

	@Override
	public void apply( final long[] source, final long[] target )
	{
		assert source.length >= numTargetDimensions && target.length >= numTargetDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
			target[ d ] = apply( ( int ) source[ d ] );
	}

	@Override
	public void apply( final int[] source, final int[] target )
	{
		assert source.length >= numTargetDimensions && target.length >= numTargetDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
			target[ d ] = apply( lut[ source[ d ] ] );
	}

	@Override
	public void apply( final Localizable source, final Positionable target )
	{
		assert source.numDimensions() >= numTargetDimensions && target.numDimensions() >= numTargetDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
			target.setPosition( apply( source.getIntPosition( d ) ), d );
	}

	@Override
	public void applyInverse( final long[] source, final long[] target )
	{
		assert source.length >= numSourceDimensions && target.length >= numSourceDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source[ d ] = applyInverse( ( int ) target[ d ] );
	}

	@Override
	public void applyInverse( final int[] source, final int[] target )
	{
		assert source.length >= numSourceDimensions && target.length >= numSourceDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source[ d ] = applyInverse( target[ d ] );
	}

	@Override
	public void applyInverse( final Positionable source, final Localizable target )
	{
		assert source.numDimensions() >= numSourceDimensions && target.numDimensions() >= numSourceDimensions: "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source.setPosition( applyInverse( target.getIntPosition( d ) ), d );
	}

	@Override
	public InvertibleTransform inverse()
	{
		return new PermutationTransform( inverseLut, numTargetDimensions, numSourceDimensions );
	}

	public PermutationTransform copyToDimension( final int numSourceDimensions, final int numTargetDimensions )
	{
		return new PermutationTransform( inverseLut, numSourceDimensions, numTargetDimensions );
	}
}
