package org.janelia.waves.thickness.opinion.symmetry;

public interface SymmetryShiftRegularizer {
	
	public double shiftAndRegularize( double[] coordinates, double[] shifts, double[] weights );

}
