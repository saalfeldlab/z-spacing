package org.janelia.thickness.inference.visitor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class LazyVisitor implements Visitor
{

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{
		// do not do anything
	}

}
