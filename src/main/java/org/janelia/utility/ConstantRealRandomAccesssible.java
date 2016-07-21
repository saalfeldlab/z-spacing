package org.janelia.utility;

import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 * @param <T>
 */
public class ConstantRealRandomAccesssible< T > implements RealRandomAccessible< T >
{

	private final int numDimensions;

	private final T value;

	/**
	 * @param numDimensions
	 * @param value
	 */
	public ConstantRealRandomAccesssible( final int numDimensions, final T value )
	{
		super();
		this.numDimensions = numDimensions;
		this.value = value;
	}

	@Override
	public int numDimensions()
	{
		return this.numDimensions;
	}

	public class ConstantRealRandomAccess extends RealPoint implements RealRandomAccess< T >
	{

		public ConstantRealRandomAccess()
		{
			super( numDimensions );
		}

		@Override
		public T get()
		{
			return value;
		}

		@Override
		public ConstantRealRandomAccess copy()
		{
			return new ConstantRealRandomAccess();
		}

		@Override
		public ConstantRealRandomAccess copyRealRandomAccess()
		{
			return copy();
		}

	}

	@Override
	public RealRandomAccess< T > realRandomAccess()
	{
		return new ConstantRealRandomAccess();
	}

	@Override
	public RealRandomAccess< T > realRandomAccess( final RealInterval interval )
	{
		return realRandomAccess();
	}

}
