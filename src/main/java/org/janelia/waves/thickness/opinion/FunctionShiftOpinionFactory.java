package org.janelia.waves.thickness.opinion;

import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.DifferentiableParameterizedFunction;
import org.janelia.waves.thickness.functions.DifferentiableParameterizedFunctionFactory;

public class FunctionShiftOpinionFactory implements OpinionFactory {
	
	private final DifferentiableParameterizedFunctionFactory functionFactory;
	private final DifferentiableMultivariateVectorialOptimizer optimizer;
	private final double[] weights;

	

	public FunctionShiftOpinionFactory(
			DifferentiableParameterizedFunctionFactory functionFactory,
			DifferentiableMultivariateVectorialOptimizer optimizer,
			double[] initialGuess, double[] weights ) {
		super();
		this.functionFactory = functionFactory;
		this.optimizer = optimizer;
		this.weights = weights;
	}



	public Opinion create(double[] coordinates, double[] measurements, Meta meta, double reference ) {
		
		double[] initialGuess = new double[] { reference, 1.0 };
		
		return new FunctionShiftOpinion( coordinates, 
				measurements, 
				this.functionFactory.create( new double[] { reference } ), 
				this.optimizer, 
				initialGuess, 
				this.weights );
	}

}
