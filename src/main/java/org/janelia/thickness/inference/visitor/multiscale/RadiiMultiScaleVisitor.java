package org.janelia.thickness.inference.visitor.multiscale;

import java.io.FileNotFoundException;
import java.util.Arrays;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;
import org.janelia.utility.io.IO;

public class RadiiMultiScaleVisitor implements MultiScaleVisitor {
	
	private final String format;
	
	/**
	 * @param format
	 */
	public RadiiMultiScaleVisitor(final String format) {
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
			final Options options) {
		
		final String data     = Arrays.toString( radii );
		final String filename = String.format( format, index );
		try {
			IO.write(data, filename);
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
