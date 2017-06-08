/**
 *
 */
package org.janelia.thickness.inference.visitor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky
 *
 */
public class GlobalCorrelationFitVisitor implements Visitor
{

	private double[] fit = null;

	public double[] getFit()
	{
		return fit;
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
			final RandomAccessibleInterval< double[] > estimatedFits,
			final double averageShift )
	{

		if ( estimatedFits == null )
			return;
		this.fit = estimatedFits.randomAccess().get();

	}

}
