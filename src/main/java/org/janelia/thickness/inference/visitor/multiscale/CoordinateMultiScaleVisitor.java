package org.janelia.thickness.inference.visitor.multiscale;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;
import org.janelia.utility.io.IO;

public class CoordinateMultiScaleVisitor implements MultiScaleVisitor {
	
	private final String format;
	
	/**
	 * @param filename
	 */
	public CoordinateMultiScaleVisitor(final String format) {
		super();
		this.format = format;
	}



	@Override
	public void act(
			final int index, 
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii, 
			final int[] steps, 
			final CorrelationsObjectInterface co,
			final Options options ) {
		final String filename = String.format( format, index );
		IO.write( lutField, filename );
	}

}
