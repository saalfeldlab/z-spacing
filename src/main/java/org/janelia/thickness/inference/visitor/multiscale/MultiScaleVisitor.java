package org.janelia.thickness.inference.visitor.multiscale;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;

public interface MultiScaleVisitor {
	
	public void act(
			final int index,
			RandomAccessibleInterval< DoubleType > lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii,
            final int[] steps,
            final CorrelationsObjectInterface co,
			final Options options
			);

}
