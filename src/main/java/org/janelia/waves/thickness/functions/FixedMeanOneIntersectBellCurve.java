package org.janelia.waves.thickness.functions;

import org.apache.commons.math.FunctionEvaluationException;

public class FixedMeanOneIntersectBellCurve implements DifferentiableParameterizedFunction {
	
	final private double mean;
	

	public FixedMeanOneIntersectBellCurve(double mean) {
		super();
		this.mean = mean;
	}

	public double value(double x, double[] parameters)
			throws FunctionEvaluationException {
		if ( parameters.length != 1 ) {
			throw new FunctionEvaluationException( parameters, "Accept exactly one parameter, got " + parameters.length );
			
		}
		
		double xCorr = x - this.mean;
		return Math.exp( - 0.5 * xCorr * xCorr / ( parameters[0] * parameters[0] ) );
	}

	public double[] gradient(double x, double[] parameters)
			throws FunctionEvaluationException {
		// TODO Auto-generated method stub
		
		double[] result = new double[1];
		
		double xCorr = x - this.mean;
		
		result[0] = this.value( x, parameters ) * xCorr * xCorr / Math.pow(parameters[0], 3.0);
		
		return result;
	}

	public double derivative(double x, double[] parameters) throws FunctionEvaluationException {
		return this.value( x, parameters) * ( this.mean - x ) / ( parameters[0] * parameters[0] );
	}

	public double[] inverse(double x, double[] parameters) throws DomainError {
		if ( x <= 0.0 || x > 1.0 ) {
			throw new DomainError();
		}
		double[] result = new double[2];
		
		double sqrt = Math.sqrt( - 2 * parameters[0] * parameters[0] * Math.log( x ) );
		
		result[0] = this.mean + sqrt;
		result[1] = this.mean - sqrt;
		
		return result;
	}

}
