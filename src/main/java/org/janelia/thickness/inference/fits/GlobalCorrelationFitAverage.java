/**
 *
 */
package org.janelia.thickness.inference.fits;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.ConstantUtils;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky
 *
 */
public class GlobalCorrelationFitAverage extends AbstractCorrelationFit
{

	private double[] summedMeasurements;

	private double[] weightSum;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.janelia.thickness.inference.fits.AbstractCorrelationFit#add(int,
	 * int, double)
	 */
	@Override
	protected void add( final int z, final int dz, final double value, final double weight )
	{
		summedMeasurements[ dz ] += value * weight;
		weightSum[ dz ] += weight;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.janelia.thickness.inference.fits.AbstractCorrelationFit#clear(int)
	 */
	@Override
	protected void init( final int size )
	{
		summedMeasurements = new double[ size + 1 ];
		weightSum = new double[ size + 1 ];
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.janelia.thickness.inference.fits.AbstractCorrelationFit#estimate()
	 */
	@Override
	protected RandomAccessibleInterval< double[] > estimate( final int size )
	{
		final double[] estimate = summedMeasurements.clone();
		estimate[ 0 ] = -1;
		for ( int z = 1; z < estimate.length; ++z )
			estimate[ z ] /= -weightSum[ z ];
		final FinalInterval fi = new FinalInterval( size );
		return Views.interval( Views.raster( ConstantUtils.constantRealRandomAccessible( estimate, 1 ) ), fi );
	}

}
