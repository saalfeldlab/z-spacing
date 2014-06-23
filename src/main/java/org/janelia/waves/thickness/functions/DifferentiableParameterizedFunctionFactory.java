package org.janelia.waves.thickness.functions;

public interface DifferentiableParameterizedFunctionFactory {
	
	public DifferentiableParameterizedFunction create( double[] parameters );
	
}
