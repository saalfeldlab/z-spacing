package org.janelia.thickness.inference.fits;

import java.util.ArrayList;

import mpicbg.models.PointMatch;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 */
public class CorrelationFitAverage extends AbstractCorrelationFit
{
	@Override
	protected double estimate( ArrayList< PointMatch > measurements )
	{
		double sum = 0.0;
		double weightSum = 0.0;
		for ( PointMatch m : measurements )
		{
			double weight = m.getWeight();
			sum += weight * m.getP2().getW()[ 0 ];
			weightSum += weight;
		}
		return sum / weightSum;
	}
}
