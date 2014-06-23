package org.janelia.waves.thickness.opinion;

public interface Opinion {
	public double[] express();
	
	public double[] express( double[] coordinates );
}
