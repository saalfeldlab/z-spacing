package org.janelia.thickness.inference.visitor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface Visitor {

	public < T extends RealType< T > > void act(
			int iteration, 
			RandomAccessibleInterval< T > matrix, 
			double[] lut,
			int[] permutation,
			int[] inversePermutation,
			double[] multipliers,
			double[] weights,
			RandomAccessibleInterval< double[] > estimatedFit
			);
}
