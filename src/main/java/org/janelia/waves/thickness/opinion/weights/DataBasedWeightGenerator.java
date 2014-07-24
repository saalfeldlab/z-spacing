package org.janelia.waves.thickness.opinion.weights;

import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;

public interface DataBasedWeightGenerator {
	public void generateAtXY(double[] zAxis,
			CorrelationsObjectInterface correlations, long zBinMin, long zBinMax, long zStart, long zEnd, int range,
			long x, long y) ;
	
	public void generate( double[] coordinates, double[] measurements, long x, long y, double zRef, long zMin, long zMax, long zPosition );
	
	public double getWeightFor( long x, long y, long zBin );
	
	
}
