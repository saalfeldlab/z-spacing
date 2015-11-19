/**
 * 
 */
package org.janelia.thickness.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import net.imglib2.util.ValuePair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class OpinionMediatorWeightedAverage implements OpinionMediator {

	@Override
	public ArrayImg<DoubleType, DoubleArray> mediate(
			final TreeMap<Long, ArrayList<ValuePair<Double, Double>>> shifts) {
		final double[] result = new double[ shifts.size() ];
		mediate( shifts, result );
		return ArrayImgs.doubles( result, result.length );
	}

	@Override
	public void mediate(
			final TreeMap<Long, ArrayList<ValuePair<Double, Double>>> shifts,
			final double[] result) {
		for ( int i = 0; i < result.length; ++i ) {
			
			final ArrayList<ValuePair<Double, Double>> localShifts = shifts.get( (long) i );
			
			double shift     = 0.0;
			double weightSum = 0.0;
			
			if ( localShifts != null ) {
				for ( final ValuePair<Double, Double> l : localShifts ) {
					final Double v = l.getA();
					final Double w = l.getB();
					shift     += w*v;
					weightSum += w;
				}
			}
			
			shift /= weightSum;
			result[ i ] = shift;
		}
	}

	@Override
	public OpinionMediatorWeightedAverage copy() {
		return new OpinionMediatorWeightedAverage();
	}

}
