package org.janelia.utility;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.DoubleType;

public class OuterProductViews
{

	public static < T extends NumericType< T > > OuterProductView< T, T, T > product( final RandomAccessibleInterval< T > a1, final RandomAccessibleInterval< T > a2 )
	{
		final T t = a1.randomAccess().get().createVariable();
		return new OuterProductView<>( a1, a2, new TypeIdentity<>(), new TypeIdentity<>(), t );
	}

	public static OuterProductView< ?, ?, DoubleType > product( final double[] arr1, final double[] arr2 )
	{
		return product( ArrayImgs.doubles( arr1, arr1.length ), ArrayImgs.doubles( arr2, arr2.length ) );
	}

}
