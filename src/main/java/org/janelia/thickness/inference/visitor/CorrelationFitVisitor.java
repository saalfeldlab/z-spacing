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
package org.janelia.thickness.inference.visitor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class CorrelationFitVisitor extends CSVVisitor
{

	public class ArrayIterable implements Iterable< String >
	{
		private final Iterable< double[] > data;

		public ArrayIterable( final Iterable< double[] > data )
		{
			this.data = data;
		}

		@Override
		public Iterator< String > iterator()
		{
			final Iterator< double[] > it = data.iterator();
			return new Iterator< String >()
			{

				@Override
				public boolean hasNext()
				{
					return it.hasNext();
				}

				@Override
				public String next()
				{
					final double[] d = it.next();
					return Arrays.toString( d ).substring( 1, d.length - 1 );
				}

			};
		}

	}

	private int range;

	public int getRange()
	{
		return range;
	}

	public void setRange( final int range )
	{
		this.range = range;
	}

	public CorrelationFitVisitor( final String basePath, final String relativeFilePattern, final String separator, final int range )
	{
		super( basePath, relativeFilePattern, separator );
		this.range = range;
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFits )
	{

		if ( estimatedFits == null )
			return;
		try
		{
			final String path = fileDir( iteration );
			createParentDirectory( path );
			final IterableInterval< double[] > iterable = Views.iterable( estimatedFits );
			write( new IndexedIterable<>( separator, new ArrayIterable( iterable ) ), path );
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
