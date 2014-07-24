package org.janelia.waves.thickness.functions;

public class OneIntersectBellCurveFactory implements
		DifferentiableParameterizedFunctionFactory {

	public DifferentiableParameterizedFunction create(double[] parameters) {
		return new OneIntersectBellCurve();
	}

}
