package org.janelia.waves.thickness.opinion.weights;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;
import org.janelia.waves.thickness.functions.symmetric.BellCurveVariableIntersect;

@SuppressWarnings("deprecation")
public class FitIterationsTest {

	public static void main ( String[] args ) throws OptimizationException, FunctionEvaluationException, IllegalArgumentException {
		double[] c = new double[] { 7.556179251850734, 9.162510323311603, 10.733289074549202, 11.377377080033643, 12.022137318806623 }; 
		double[] m = new double[] { 0.8824969025845955, 0.7753865875810754, 1.0, 0.9692332344763441, 0.8824969025845955 };
		double[] i = new double[] { 10.733289074549202, 2.775342364543257, 0.9625888076754706 };
		double[] j = new double[] { 10.733289074549202, 2.775342364543257 };
		double[] w = new double[] { 0.9999999999547179, 0.9999999940402464, 0.9999999999125867, 1.0, 1.0 };
		
		CurveFitter f = new CurveFitter( new LevenbergMarquardtOptimizer() );
		
		for ( int idx = 0; idx < c.length; ++idx ) {
			f.addObservedPoint( w[idx], c[idx], m[idx] );
		}
		
		f.fit( new BellCurve(), j );
		
		f.fit( new BellCurveVariableIntersect(), i );
	}
}
