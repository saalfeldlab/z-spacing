package org.janelia.utility;

import java.util.Arrays;

import net.imglib2.AbstractWrappedInterval;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.NumericType;

// right now this works only for 1d vector

public class OuterProductView< T, U, V extends NumericType< V > > extends AbstractWrappedInterval< FinalInterval > implements RandomAccessibleInterval< V >
{

	private final static int N_DIM = 2;

	private final RandomAccessibleInterval< T > vec1;

	private final RandomAccessibleInterval< U > vec2;

	private final Converter< T, V > c1;

	private final Converter< U, V > c2;

	private final V v;

	private final long[] dim;

	private final long[] max;

	private final long[] min;

	public OuterProductView( final RandomAccessibleInterval< T > vec1, final RandomAccessibleInterval< U > vec2, final Converter< T, V > c1, final Converter< U, V > c2, final V v )
	{
		super( new FinalInterval( vec2.dimension( 0 ), vec1.dimension( 0 ) ) );
		this.vec1 = vec1;
		this.vec2 = vec2;
		this.c1 = c1;
		this.c2 = c2;
		this.v = v.createVariable();
		this.dim = new long[] { vec2.dimension( 0 ), vec1.dimension( 0 ) };
		this.max = Arrays.stream( this.dim ).map( l -> l - 1 ).toArray();
		this.min = new long[ N_DIM ];
	}

	@Override
	public RandomAccess< V > randomAccess()
	{
		return new OuterProductAccess();
	}

	@Override
	public RandomAccess< V > randomAccess( final Interval interval )
	{
		return randomAccess();
	}

	// implementing Point is inefficient, create custom RA
	public class OuterProductAccess extends Point implements RandomAccess< V >
	{

		private final RandomAccess< T > a1;

		private final RandomAccess< U > a2;

		private final V v1;

		private final V v2;

		private OuterProductAccess()
		{
			super( N_DIM );
			this.a1 = vec1.randomAccess();
			this.a2 = vec2.randomAccess();
			this.v1 = v.createVariable();
			this.v2 = v.createVariable();
		}

		private OuterProductAccess( final OuterProductAccess other )
		{
			super( other.position.clone() );
			this.a1 = other.a1.copyRandomAccess();
			this.a2 = other.a2.copyRandomAccess();
			this.v1 = other.v1.copy();
			this.v2 = other.v2.copy();
		}

		@Override
		public V get()
		{
			a1.setPosition( this.position[ 1 ], 0 );
			a2.setPosition( this.position[ 0 ], 0 );
			c1.convert( a1.get(), v1 );
			c2.convert( a2.get(), v2 );
			v1.mul( v2 );
			return v1;
		}

		@Override
		public OuterProductAccess copy()
		{
			return copyRandomAccess();
		}

		@Override
		public OuterProductAccess copyRandomAccess()
		{
			return new OuterProductAccess( this );
		}

	}

}
