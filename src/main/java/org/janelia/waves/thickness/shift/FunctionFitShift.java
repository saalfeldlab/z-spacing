package org.janelia.waves.thickness.shift;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.exception.InconsistencyError;
import org.janelia.exception.ShiftEstimationException;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

@SuppressWarnings("deprecation")
public class FunctionFitShift implements ShiftEstimate {
	
	private double[] initialGuess;
	final private SymmetricDifferentiableParameterizedFunction func;
	final private CurveFitter fitter;
	
	
	
	public FunctionFitShift(double[] initialGuess,
			SymmetricDifferentiableParameterizedFunction func, CurveFitter fitter) {
		super();
		this.initialGuess = initialGuess;
		this.func = func;
		this.fitter = fitter;
	}



	public double estimateShift( double[] coordinates, double[] measurements, double[] weights, double referenceCoordinate ) throws InconsistencyError, ShiftEstimationException {
		
		if ( coordinates.length != measurements.length || measurements.length != weights.length ) {
			
			throw new InconsistencyError();
			
		}
		
		fitter.clearObservations();
		
		for ( int i = 0; i < coordinates.length; ++i ) {
			fitter.addObservedPoint( weights[i], coordinates[i], measurements[i] );
		}
		
		try {
			this.initialGuess = fitter.fit( func, this.initialGuess );
		} catch (OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ShiftEstimationException();
		} catch (FunctionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ShiftEstimationException();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ShiftEstimationException();
		}

		
		return func.axisOfSymmetry( this.initialGuess ) - referenceCoordinate;
	}

}
