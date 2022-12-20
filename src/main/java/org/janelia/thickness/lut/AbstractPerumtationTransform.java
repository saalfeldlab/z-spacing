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

import net.imglib2.transform.InvertibleTransform;

/**
 * Bijective integer transform mapping between integer coordinates in [0,n-1].
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public abstract class AbstractPerumtationTransform implements
		InvertibleTransform
{

	final protected int[] lut;

	final protected int[] inverseLut;

	/**
	 * @param lut
	 *            must be a bijective permutation over its index set, i.e. for a
	 *            lut of legnth n, the sorted content the array must be
	 *            [0,...,n-1] which is the index set of the lut.
	 */
	public AbstractPerumtationTransform( final int[] lut )
	{
		super();
		this.lut = lut.clone();

		inverseLut = new int[ lut.length ];
		for ( int i = 0; i < lut.length; ++i )
			inverseLut[ lut[ i ] ] = i;
	}

	public int apply( final int x )
	{
		return lut[ x ];
	}

	public long applyChecked( final int x )
	{
		if ( x < 0 )
			return -Long.MAX_VALUE;
		else if ( x >= lut.length )
			return Long.MAX_VALUE;
		else
			return apply( x );
	}

	public int applyInverse( final int y )
	{
		return inverseLut[ y ];
	}

	public long applyInverseChecked( final int y )
	{
		if ( y < 0 )
			return -Long.MAX_VALUE;
		else if ( y >= lut.length )
			return Long.MAX_VALUE;
		else
			return applyInverse( y );
	}

	public int[] getLutCopy()
	{
		return lut.clone();
	}

	public int[] getInverseLutCopy()
	{
		return inverseLut.clone();
	}

}
