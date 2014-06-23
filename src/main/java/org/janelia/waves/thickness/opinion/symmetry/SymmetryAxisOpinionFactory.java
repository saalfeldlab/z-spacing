package org.janelia.waves.thickness.opinion.symmetry;

import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.waves.thickness.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;
import org.janelia.waves.thickness.opinion.Opinion;
import org.janelia.waves.thickness.opinion.OpinionFactory;
import org.janelia.waves.thickness.opinion.WeightGenerator;

public class SymmetryAxisOpinionFactory implements OpinionFactory {

	private final SymmetricDifferentiableParameterizedFunction func;
	private final DifferentiableMultivariateVectorialOptimizer optimizer;
	private final WeightGenerator weightGenerator;

	public SymmetryAxisOpinionFactory(
			SymmetricDifferentiableParameterizedFunction func,
			DifferentiableMultivariateVectorialOptimizer optimizer,
			WeightGenerator weightGenerator) {
		super();
		this.func = func;
		this.optimizer = optimizer;
		this.weightGenerator = weightGenerator;
	}


	public Opinion create(double[] coordinates, double[] measurements, Meta meta, double reference ) {
		
		double[] initialGuess = new double[] { reference, 1.0 };
		
		return new SymmetryAxisOpinion( this.func, measurements, new CurveFitter(this.optimizer), initialGuess, weightGenerator, reference );
		
	}

}
