/**
 * 
 */
package org.janelia.thickness.inference.fits;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.ConstantUtils;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class GlobalCorrelationFitAverage extends AbstractCorrelationFit
{

	private double[] summedMeasurements;

	private int[] nSamples;

	/* (non-Javadoc)
	 * @see org.janelia.thickness.inference.fits.AbstractCorrelationFit#add(int, int, double)
	 */
	@Override
	protected void add( int z, int dz, double value )
	{
		summedMeasurements[ dz ] += value;
		++nSamples[ dz ];

	}

	/* (non-Javadoc)
	 * @see org.janelia.thickness.inference.fits.AbstractCorrelationFit#clear(int)
	 */
	@Override
	protected void init( int size )
	{
		summedMeasurements = new double[ size + 1 ];
		nSamples = new int[ size + 1 ];
	}

	/* (non-Javadoc)
	 * @see org.janelia.thickness.inference.fits.AbstractCorrelationFit#estimate()
	 */
	@Override
	protected RandomAccessibleInterval< double[] > estimate( int size )
	{
		double[] estimate = summedMeasurements.clone();
		estimate[ 0 ] = -1;
		for ( int z = 1; z < estimate.length; ++z )
			estimate[ z ] /= -nSamples[ z ];
		FinalInterval fi = new FinalInterval( size );
		return Views.interval( Views.raster( ConstantUtils.constantRealRandomAccessible( estimate, 1 ) ), fi );
	}

}
