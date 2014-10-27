package org.janelia.correlations;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface CrossCorrelationFactoryInterface< T extends RealType<T>, U extends RealType<U>, S extends RealType<S> & NativeType<S> > {
	
	public RandomAccessibleInterval< S > 
	create( RandomAccessibleInterval< T > image1,
			RandomAccessibleInterval< U > image2,
			final long[] radius );

}
