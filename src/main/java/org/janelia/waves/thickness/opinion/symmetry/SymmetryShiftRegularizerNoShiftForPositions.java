package org.janelia.waves.thickness.opinion.symmetry;

import java.util.ArrayList;
import java.util.Arrays;

public class SymmetryShiftRegularizerNoShiftForPositions implements
		SymmetryShiftRegularizer {
	
	private final ArrayList<Integer> ignoreIndices;

	public SymmetryShiftRegularizerNoShiftForPositions(
			ArrayList<Integer> ignoreIndices) {
		super();
		this.ignoreIndices = ignoreIndices;
	}

	public double shiftAndRegularize(double[] coordinates, double[] shifts,
			double[] weights) {
		double change = 0.0;
		
		for ( int idx = 0; idx < shifts.length; ++idx ) {
			
			double currShift = shifts[idx] * weights[idx];
			
			
			
			if ( ignoreIndices.contains( idx ) ) {
				currShift = 0.0;
			}
			
			change += Math.abs( currShift );
			coordinates[idx] += currShift;
			
		}
		
		return change;
	}

}
