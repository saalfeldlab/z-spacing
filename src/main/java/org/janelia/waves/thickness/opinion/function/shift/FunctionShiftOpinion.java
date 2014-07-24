package org.janelia.waves.thickness.opinion.function.shift;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.waves.thickness.functions.DifferentiableParameterizedFunction;
import org.janelia.waves.thickness.functions.DomainError;
import org.janelia.waves.thickness.opinion.Opinion;

@SuppressWarnings("deprecation")
public class FunctionShiftOpinion implements Opinion {
	
	private final double[] coordinates;
	private final double[] measurements;
	private final DifferentiableParameterizedFunction function;
	private final CurveFitter fitter;
	private final double[] shifts;
	
	
	private double[] parameters;
	private double[] initialGuess;
	private double[] weights;
	
	

	public FunctionShiftOpinion(double[] coordinates, double[] measurements,
			DifferentiableParameterizedFunction function,
			DifferentiableMultivariateVectorialOptimizer optimizer,
			double[] initialGuess,
			double[] weights) {
		super();
		this.coordinates  = coordinates;
		this.measurements = measurements;
		this.function     = function;
		this.fitter       = new CurveFitter( optimizer );
		this.initialGuess = initialGuess;
		this.weights      = weights;
		this.shifts       = new double[this.coordinates.length];
		
	}



	public double[] express() {
		
		this.fitter.clearObservations();
		
		for ( int i = 0; i < coordinates.length; ++i ) {
			this.fitter.addObservedPoint( this.weights[i], this.coordinates[i], this.measurements[i] );
		}
		
		
		try {
			this.parameters = this.fitter.fit( this.function, this.initialGuess );
		} catch (OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for ( int i = 0; i < this.parameters.length; ++i ) {
			this.initialGuess[i] = this.parameters[i];
		}
		
		for ( int z = 0; z < this.shifts.length; ++z ) {
			double[] newZ = null;
			try {
				newZ = this.function.inverse( measurements[z], this.parameters );
			} catch (DomainError e) {
				newZ = new double[]{ coordinates[z] };
			} finally {
				this.shifts[z] = newZ[0] - coordinates[z];
				for ( int i = 1; i < newZ.length; ++i ) {
					if ( Math.abs( newZ[i] - coordinates[z] ) < Math.abs( this.shifts[z]) ) {
						this.shifts[z] = newZ[i] - coordinates[z];
					}
				}
			}
		}
		
		return this.shifts;
	}



	public double[] express(double[] coordinates) {
		return this.express();
	}



	public double[] express(double[] coordinates, double[] weights) {
		this.weights = weights;
		return express( coordinates );
	}

}
