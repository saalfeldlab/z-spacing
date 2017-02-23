package org.janelia.thickness.inference.fits;

import net.imglib2.RandomAccessibleInterval;

/**
 *
 * @author Philipp Hanslovsky
 *
 */

public class GlobalCorrelationFitAverageRegularized extends AbstractCorrelationFit
{

	private double[] reg;

	private final double lambda;

	private final GlobalCorrelationFitAverage fit = new GlobalCorrelationFitAverage();

	public GlobalCorrelationFitAverageRegularized( final double[] reg, final double lambda )
	{
		super();
		this.reg = reg;
		this.lambda = lambda;
	}

	@Override
	protected void add( final int z, final int dz, final double value, final double weight )
	{
		fit.add( z, dz, value, weight );
	}

	@Override
	protected void init( final int size )
	{
		fit.init( size );
	}

	@Override
	protected RandomAccessibleInterval< double[] > estimate( final int size )
	{
		final RandomAccessibleInterval< double[] > rai = fit.estimate( size );
		final double oneMinusLambda = 1.0 - lambda;
		final double[] current = rai.randomAccess().get();
		for ( int i = 0; i < Math.min( current.length, this.reg.length ); ++i )
		{
			final double v1 = current[ i ];
			final double v2 = reg[ i ];
			current[ i ] = Double.isNaN( v1 ) ? v2 : Double.isNaN( v2 ) ? v1 : oneMinusLambda * v1 + lambda * v2;
		}
		// TODO Do this or not?
		this.reg = current;
		return rai;
	}

}
