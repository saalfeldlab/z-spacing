package org.janelia.waves.thickness.opinion;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.janelia.waves.thickness.functions.DifferentiableParameterizedFunction;
import org.janelia.waves.thickness.functions.FixedMeanOneIntersectBellCurve;

public class ShiftToFitOpinion implements Opinion {
	
	private final CurveFitter fit;
	private final DifferentiableParameterizedFunction func;
	private final double[] coordinates;
	private final double[] values;
	private double[] weights;
	
	
	public ShiftToFitOpinion( CurveFitter fit, 
			DifferentiableParameterizedFunction func,
			final double[] coordinates,
			final double[] values,
			final double[] weights
			) {
		super();
		this.fit         = fit;
		this.func        = func;
		this.coordinates = coordinates;
		this.values      = values;
		this.weights     = weights;
		
		if ( coordinates.length != values.length || values.length != weights.length ) {
			throw new RuntimeException( "Inconsitent array sizes!");
		}
	}
	
	public ShiftToFitOpinion( DifferentiableParameterizedFunction func,
			final double[] coordinates,
			final double[] values,
			final double[] weights
			) {
		this( new CurveFitter( new LevenbergMarquardtOptimizer() ), 
				func, 
				coordinates, 
				values,
				weights);
	}
	
		
	public ShiftToFitOpinion(final double[] coordinates,
			final double[] values,
			final double[] weights
			) {
		this( new CurveFitter( new LevenbergMarquardtOptimizer() ), 
				new FixedMeanOneIntersectBellCurve( 0.0 ),
				coordinates,
				values,
				weights);
	}

	public double[] express() {
		
		this.fit.clearObservations();
		for ( int i = 0; i < this.values.length; ++i ) {
			this.fit.addObservedPoint( this.weights[i], this.coordinates[i], this.values[i]);
		}
		
		try {
			this.fit.fit( this.func, new double[]{ 1.0 } );
		} catch (@SuppressWarnings("deprecation") OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		// TODO Auto-generated method stub
		return null;
	}

	public double[] express(double[] coordinates) {
		return this.express();
	}

	public double[] express(double[] coordinates, double[] weights) {
		this.weights = weights;
		return express( coordinates );
	}

}
