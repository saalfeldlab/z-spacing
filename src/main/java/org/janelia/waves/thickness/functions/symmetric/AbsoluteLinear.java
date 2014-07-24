package org.janelia.waves.thickness.functions.symmetric;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.waves.thickness.functions.DomainError;

public class AbsoluteLinear implements
		SymmetricDifferentiableParameterizedFunction {

	public double derivative(double x, double[] parameters)
			throws FunctionEvaluationException {
		if ( x < parameters[0] ) {
			return -parameters[2]; 
		} else if ( x > parameters[0] ) {
			return parameters[2];
		} else {
			return Double.NaN;
		}
	}

	public double[] inverse(double x, double[] parameters) throws DomainError {
		double[] result = new double[2];
		result[0] = ( x - parameters[1] ) / parameters[2] + parameters[0];
		result[1] = parameters[0] - result[0];
		return result;
	}

	public double value(double x, double[] parameters)
			throws FunctionEvaluationException {
		return parameters[1] + parameters[2] * Math.abs( x -parameters[0] );
	}

	public double[] gradient(double x, double[] parameters)
			throws FunctionEvaluationException {
		double[] result = new double[] { parameters[2], 1, Math.abs( x - parameters[0] ) };
		if ( x < parameters[0] )
			result[0] = -result[0];
		if ( x == parameters[0] )
			result[0] = Double.NaN;
		return result;
	}

	public double axisOfSymmetry(double[] parameters) {
		return parameters[0];
	}

}
