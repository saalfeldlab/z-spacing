package org.janelia.thickness.inference.visitor;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

public class LazyVisitor implements Visitor {

	@Override
	public void act(final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix,
			final double[] lut, final AbstractLUTRealTransform transform,
			final double[] multipliers, final double[] weights, final double[] estimatedFit,
			final int[] positions) {
		// do not do anything
	}

}
