package org.janelia.waves.thickness.functions.symmetric;

import org.janelia.waves.thickness.functions.DifferentiableParameterizedFunction;

public interface SymmetricDifferentiableParameterizedFunction extends
		DifferentiableParameterizedFunction {

	public double axisOfSymmetry( double[] parameters );
	
}
