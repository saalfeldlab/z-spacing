package org.janelia.waves.thickness.opinion.weights;

public class IgnoreIdentityWeightGenerator implements WeightGenerator {
	
	
	public double[] generate(double[] coordinates, double zRef) {
		double[] res = new double[ coordinates.length ];
		
		for ( int idx = 0; idx < res.length; ++idx ) {
			res[idx] = 1.0;
			if ( Math.abs( zRef - coordinates[idx] ) < 0.001 ) {
				res[idx] = 0.0;
			}
					
		}
		
		return res;
	}
	
	public double[] generate(long zMin, long zMax, long zRef) {
		double[] res = new double[ (int) (zMax - zMin) ];
		
		for ( int idx = 0; idx < res.length; ++idx ) {
			res[idx] = 1.0;
		}
		
		return res;
	}
	
}