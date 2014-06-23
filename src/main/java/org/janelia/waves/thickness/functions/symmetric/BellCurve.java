package org.janelia.waves.thickness.functions.symmetric;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.waves.thickness.functions.DomainError;

public class BellCurve implements SymmetricDifferentiableParameterizedFunction {

	public double value(double x, double[] parameters)
			throws FunctionEvaluationException {
		if ( parameters.length != 2 ) {
			throw new FunctionEvaluationException( parameters, "Accept exactly two parameters, got " + parameters.length );
			
		}
		
		double xCorr = x - parameters[0];
		return Math.exp( - 0.5 * xCorr * xCorr / ( parameters[1] * parameters[1] ) );
	}

	public double[] gradient(double x, double[] parameters)
			throws FunctionEvaluationException {
		// TODO Auto-generated method stub
		
		double[] result = new double[2];
		
		double xCorr = x - parameters[0];
		
		result[0] = this.value( x, parameters ) * xCorr / Math.pow( parameters[1], 2.0 );
		
		result[1] = this.value( x, parameters ) * xCorr * xCorr / Math.pow(parameters[1], 3.0);
		
		return result;
	}

	public double derivative(double x, double[] parameters) throws FunctionEvaluationException {
		return this.value( x, parameters) * ( parameters[0] - x ) / ( parameters[1] * parameters[1] );
	}

	public double[] inverse(double x, double[] parameters) throws DomainError {
		if ( x <= 0.0 || x > 1.0 ) {
			throw new DomainError();
		}
		double[] result = new double[2];
		
		double sqrt = Math.sqrt( - 2 * parameters[1] * parameters[1] * Math.log( x ) );
		
		result[0] = parameters[0] + sqrt;
		result[1] = parameters[0] - sqrt;
		
		return result;
	}

	public double axisOfSymmetry(double[] parameters) {
		return parameters[0];
	}


}
