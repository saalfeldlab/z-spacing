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
import java.util.Iterator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ScalingFactorsVisitor extends CSVVisitor
{

	public class DoubleArrayIterable implements Iterable< Double >
	{

		private final double[] data;

		DoubleArrayIterable( final double[] data )
		{
			this.data = data;
		}

		@Override
		public Iterator< Double > iterator()
		{
			return new Iterator< Double >()
			{
				int i = 0;

				@Override
				public boolean hasNext()
				{
					return i < data.length;
				}

				@Override
				public Double next()
				{
					return data[ i++ ];
				}

			};
		}

	}

	public ScalingFactorsVisitor( final String basePath, final String relativeFilePattern, final String separator )
	{
		super( basePath, relativeFilePattern, separator );
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] scalingFactors,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{
		try
		{
			final String path = fileDir( iteration );
			createParentDirectory( path );
			write( new IndexedIterable<>( separator, new DoubleArrayIterable( scalingFactors ) ), path );
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
