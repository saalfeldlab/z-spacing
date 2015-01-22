package org.janelia.thickness.inference.visitor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class LazyVisitor implements Visitor {

	@Override
	public < T extends RealType< T > > void act(
			final int iteration, 
			final RandomAccessibleInterval< T > matrix, 
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit
			) {
		// do not do anything
	}

}
