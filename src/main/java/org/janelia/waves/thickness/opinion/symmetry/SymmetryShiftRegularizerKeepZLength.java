package org.janelia.waves.thickness.opinion.symmetry;

public class SymmetryShiftRegularizerKeepZLength implements
		SymmetryShiftRegularizer {
	
	private final double zLength;
	
	

	public SymmetryShiftRegularizerKeepZLength(double zLength) {
		super();
		this.zLength = zLength;
	}
	
	



	public SymmetryShiftRegularizerKeepZLength( long zMin, long zMax ) {
		this( zMax - zMin );
	}





	public double shiftAndRegularize(double[] coordinates, double[] shifts,
			double[] weights) {
		
		double length = 0.0;
		double change = 0.0;
		
		for ( int idx = 0; idx < shifts.length; ++idx ) {
			length += shifts[idx];
			change += Math.abs( shifts[idx] );
		}
		
		for ( int idx = 0; idx < coordinates.length; ++idx ) {
			coordinates[idx] += zLength / ( zLength + length ) * shifts[idx];
		}
		
		return change;
		
	}

}
