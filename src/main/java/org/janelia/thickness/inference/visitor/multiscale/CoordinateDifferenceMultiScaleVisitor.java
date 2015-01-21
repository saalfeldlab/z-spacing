package org.janelia.thickness.inference.visitor.multiscale;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.correlations.storage.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;
import org.janelia.utility.io.IO;

public class CoordinateDifferenceMultiScaleVisitor implements
		MultiScaleVisitor {
	
	private final String format;

	/**
	 * @param format
	 */
	public CoordinateDifferenceMultiScaleVisitor(final String format) {
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
		final ArrayImg<DoubleType, DoubleArray> diff = ArrayImgs.doubles( lutField.dimension( 0 ), lutField.dimension( 1 ), lutField.dimension( 2 ) );
		final Cursor<DoubleType> l      = Views.flatIterable( lutField ).cursor();
		final Cursor<DoubleType> p      = Views.flatIterable( previousLutField ).cursor();
		final ArrayCursor<DoubleType> d = diff.cursor();
		
		while( d.hasNext() )
			d.next().set( l.next().get() - p.next().get() );
		final String filename = String.format( format, index );
		IO.write( diff, filename );
		
	}

}
