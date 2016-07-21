/**
 * 
 */
package org.janelia.thickness.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.util.ValuePair;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class OpinionMediatorWeightedAverage implements OpinionMediator
{

	@Override
	public void mediate(
			final TreeMap< Long, ArrayList< ValuePair< Double, Double > > > shifts,
			final double[] result )
	{
		for ( int i = 0; i < result.length; ++i )
		{

			final ArrayList< ValuePair< Double, Double > > localShifts = shifts.get( ( long ) i );

			double shift = 0.0;
			double weightSum = 0.0;

			if ( localShifts != null )
			{
				for ( final ValuePair< Double, Double > l : localShifts )
				{
					final Double v = l.getA();
					final Double w = l.getB();
					shift += w * v;
					weightSum += w;
				}
			}

			shift /= weightSum;
			result[ i ] = shift;
		}
	}

	@Override
	public OpinionMediatorWeightedAverage copy()
	{
		return new OpinionMediatorWeightedAverage();
	}

}
