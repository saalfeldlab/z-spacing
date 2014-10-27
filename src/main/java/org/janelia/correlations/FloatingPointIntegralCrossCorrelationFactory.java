package org.janelia.correlations;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.CrossCorrelationType;
import org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException;

public class FloatingPointIntegralCrossCorrelationFactory< T extends RealType< T >, U extends RealType< U >, S extends RealType< S > & NativeType< S > > implements
		CrossCorrelationFactoryInterface<T, U, S> {
	
	private final CrossCorrelationType ccType;
	private final S type;
	
	

	/**
	 * @param ccType
	 * @param type
	 */
	public FloatingPointIntegralCrossCorrelationFactory(
			final CrossCorrelationType ccType, final S type) {
		super();
		this.ccType = ccType;
		this.type = type;
	}



	@Override
	public RandomAccessibleInterval<S> create(
			final RandomAccessibleInterval<T> image1,
			final RandomAccessibleInterval<U> image2, final long[] radius) {
		// TODO Auto-generated method stub
		try {
			return new FloatingPointIntegralCrossCorrelation< T, U, S >(image1, image2, radius, ccType, type );
		} catch (final NotEnoughSpaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
