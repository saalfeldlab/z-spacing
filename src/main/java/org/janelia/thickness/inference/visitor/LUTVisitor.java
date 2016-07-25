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
public class LUTVisitor extends CSVVisitor
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

	public LUTVisitor( final String basePath, final String relativeFilePattern, final String separator )
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
			write( new IndexedIterable<>( separator, new DoubleArrayIterable( lut ) ), path );
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
