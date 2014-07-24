package org.janelia.waves.thickness.opinion.symmetry;

import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;
import org.janelia.waves.thickness.opinion.Opinion;
import org.janelia.waves.thickness.opinion.OpinionFactory;
import org.janelia.waves.thickness.opinion.weights.WeightGenerator;

public class SymmetryAxisOpinionFactory implements OpinionFactory {

	private final SymmetricDifferentiableParameterizedFunction func;
	private final DifferentiableMultivariateVectorialOptimizer optimizer;
	private final WeightGenerator weightGenerator;
	private double[] initialGuess;
	
	public SymmetryAxisOpinionFactory(
			SymmetricDifferentiableParameterizedFunction func,
			DifferentiableMultivariateVectorialOptimizer optimizer,
			WeightGenerator weightGenerator ) {
		this( func, optimizer, weightGenerator, new double[]{ 1.0, 1.0 } );
	}

	public SymmetryAxisOpinionFactory(
			SymmetricDifferentiableParameterizedFunction func,
			DifferentiableMultivariateVectorialOptimizer optimizer,
			WeightGenerator weightGenerator,
			double[] initialGuess ) {
		super();
		this.func = func;
		this.optimizer = optimizer;
		this.weightGenerator = weightGenerator;
		this.initialGuess = initialGuess;
	}


	public Opinion create(double[] coordinates, double[] measurements, Meta meta, double reference ) {
		
		initialGuess[0] = reference;
				
		return new SymmetryAxisOpinion( this.func, measurements, new CurveFitter(this.optimizer), initialGuess, weightGenerator, reference );
		
	}

}
