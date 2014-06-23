package org.janelia.waves.thickness.functions;

public class FixedMeanOneIntersectBellCurveFactory implements
		DifferentiableParameterizedFunctionFactory {

	public DifferentiableParameterizedFunction create(double[] parameters) {
		return new FixedMeanOneIntersectBellCurve( parameters[0] );
	}

}
