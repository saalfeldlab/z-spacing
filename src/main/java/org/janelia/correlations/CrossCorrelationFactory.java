/**
 * 
 */
package org.janelia.correlations;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CrossCorrelationFactory<T extends RealType<T>, U extends RealType<U>, S extends RealType<S> & NativeType<S>> implements
		CrossCorrelationFactoryInterface< T, U, S> {
	
	private final S type;

	/**
	 * @param type
	 */
	public CrossCorrelationFactory(final S type) {
		super();
		this.type = type;
	}

	@Override
	public RandomAccessibleInterval<S> create(
			final RandomAccessibleInterval<T> image1,
			final RandomAccessibleInterval<U> image2,
			final long[] radius ) {
		return new CrossCorrelation<T, U, S>(image1, image2, radius, type);
	}

}
