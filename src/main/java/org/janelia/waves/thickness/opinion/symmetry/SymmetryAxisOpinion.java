package org.janelia.waves.thickness.opinion.symmetry;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;
import org.janelia.waves.thickness.opinion.Opinion;
import org.janelia.waves.thickness.opinion.weights.WeightGenerator;

@SuppressWarnings("deprecation")
public class SymmetryAxisOpinion implements Opinion {
	
	private final SymmetricDifferentiableParameterizedFunction func;
	private final double[] measurements;
	private final CurveFitter fitter;
	private final WeightGenerator weightGenerator;
	
	private double[] initialGuess;
	private double   symmetryAxis;
	
	

	public SymmetryAxisOpinion(
			SymmetricDifferentiableParameterizedFunction func,
			double[] measurements, CurveFitter fitter,
			double[] initialGuess, WeightGenerator weightGenerator,
			double symmetryAxis) {
		super();
		this.func = func;
		this.measurements = measurements;
		this.fitter = fitter;
		this.initialGuess = initialGuess;
		this.weightGenerator = weightGenerator;
		this.symmetryAxis = symmetryAxis;
	}



	public double[] express( double[] coordinates ) {
		
		this.fitter.clearObservations();
		
		double[] weights = weightGenerator.generate( coordinates, this.symmetryAxis );
		
		for ( int i = 0; i < coordinates.length; ++i ) {
			
			fitter.addObservedPoint( weights[i], coordinates[i], this.measurements[i] );
			
		}
		
		try {
			this.initialGuess = fitter.fit( this.func, this.initialGuess );
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
		
		double oldSymmetryAxis = this.symmetryAxis;
		
		this.symmetryAxis = this.func.axisOfSymmetry( this.initialGuess );
		
		
		return new double[]{ this.symmetryAxis - oldSymmetryAxis };
	}



	public double[] express() {
		double[] coordinates = new double[ this.measurements.length ];
		
		for ( int i = 0; i < coordinates.length; ++ i ) {
			coordinates[i] = i;
		}
		
		return this.express(coordinates);
	}



	public double[] express(double[] coordinates, double[] weights) {
this.fitter.clearObservations();
		
		for ( int i = 0; i < coordinates.length; ++i ) {
			
			fitter.addObservedPoint( weights[i], coordinates[i], this.measurements[i] );
			
		}
		
		try {
			this.initialGuess = fitter.fit( this.func, this.initialGuess );
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
		
		double oldSymmetryAxis = this.symmetryAxis;
		
		this.symmetryAxis = this.func.axisOfSymmetry( this.initialGuess );
		
		
		return new double[]{ this.symmetryAxis - oldSymmetryAxis };
	}

}
