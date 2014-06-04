package org.janelia.scaling;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * This interface takes an {@link ArrayList} of {@link net.imglib2.RandomAccessibleInterval}
 * and returns a single {@link RandomAccessibleInterval}.
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> Data type of the {@link RandomAccessibleInterval}.
 */
public interface Averager< T extends NativeType<T> & RealType< T > > {
	/**
	 * 
	 * @param input list of {@link RandomAccessibleInterval}s that should be averaged.
	 * @return Average of input, potentially scaled in its dimensions.
	 */
	public RandomAccessibleInterval< T > average(final ArrayList< RandomAccessibleInterval< T > >input);
}
