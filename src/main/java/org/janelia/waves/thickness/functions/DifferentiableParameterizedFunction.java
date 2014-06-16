package org.janelia.waves.thickness.functions;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;

public interface DifferentiableParameterizedFunction extends
		ParametricRealFunction {
	public double derivative( double x, double[] parameters ) throws FunctionEvaluationException;
	
	public double[] inverse( double x, double[] parameters ) throws DomainError;
}
