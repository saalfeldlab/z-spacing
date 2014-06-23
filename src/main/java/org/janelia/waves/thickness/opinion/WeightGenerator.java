package org.janelia.waves.thickness.opinion;

public interface WeightGenerator {
	public double[] generate( long zMin, long zMax, long zRef );
	
	public double[] generate( double[] coordinates, double zRef );
}
