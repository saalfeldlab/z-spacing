package org.janelia.thickness.inference.visitor.multiscale;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.inference.Options;

public class LazyMultiScaleVisitor implements MultiScaleVisitor {

	@Override
	public void act(
			final int index,
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii, 
			final int[] steps, 
			final Options options ) {
		// do not do anything
	}

}
