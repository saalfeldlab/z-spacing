/**
 * 
 */
package org.janelia.correlations;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.CrossCorrelationType;
import org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class IntegralCrossCorrelationFactory< T extends RealType<T>, U extends RealType<U>, S extends RealType<S> & NativeType<S>, I extends RealType< I > & NativeType< I > > implements
		CrossCorrelationFactoryInterface< T, U, S > {
	
	private final CrossCorrelationType ccType;
	private final S type;
	private final I dummy;
	private final Converter< T, I > converterT;
	private final Converter< U, I > converterU;

	/**
	 * @param ccType
	 * @param type
	 * @param dummy
	 * @param converterT
	 * @param converterU
	 */
	public IntegralCrossCorrelationFactory(final CrossCorrelationType ccType, final S type,
			final I dummy, final Converter<T, I> converterT, final Converter<U, I> converterU) {
		super();
		this.ccType = ccType;
		this.type = type;
		this.dummy = dummy;
		this.converterT = converterT;
		this.converterU = converterU;
	}

	@Override
	public  RandomAccessibleInterval<S> create(
			final RandomAccessibleInterval<T> image1,
			final RandomAccessibleInterval<U> image2, final long[] radius) {
		try {
			return new IntegralCrossCorrelation< T, U, S, I>( image1, image2, radius, ccType, converterT, converterU, type, dummy );
		} catch (final NotEnoughSpaceException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

}
