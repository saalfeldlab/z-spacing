package org.janelia.waves.thickness.opinion.weights;

import java.util.TreeMap;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

@SuppressWarnings("deprecation")
public class IterativeFitWeightGenerator extends FitWeightGenerator {
	
	private final double threshold;
	private final int nIterations;
	
	

	public IterativeFitWeightGenerator(CurveFitter fitter,
			SymmetricDifferentiableParameterizedFunction func,
			double[] initialGuess, double threshold, int nIterations) {
		super(fitter, func, initialGuess);
		this.threshold = threshold;
		this.nIterations = nIterations;
	}

	public void generate(double[] coordinates, double[] measurements, long x, long y,
			double zRef, long zMin, long zMax, long zPosition) {
		
		this.initialGuess[0] = zRef;
		
		double currentChange = Double.MAX_VALUE;
		
		double[] weights = new double[ coordinates.length ];
		
		for ( int i = 0; i < measurements.length; ++i ) {
			weights[i] = this.getWeightFor( x, y, zMin + i);
		}
		
		for ( int iteration = 0; iteration < nIterations && currentChange > threshold; ++iteration ) {
			
			currentChange = 0.0;
			
		
			this.fitter.clearObservations();
			
			for (int i = 0; i < measurements.length; ++i ) {
				weights[i] = this.getWeightFor( x, y, zMin + i );
				this.fitter.addObservedPoint( weights[i], coordinates[i], measurements[i]);
			}
			
			try {
				this.initialGuess = this.fitter.fit( this.func, this.initialGuess );
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
		
			
			for (int i = 0; i < measurements.length; ++i ) {

				long currPos = i + zMin;
				
				TreeMap<Long, TreeMap<Long, Double> > opinionsAtXY = opinions.get( new ConstantPair<Long, Long>( x, y ) );
				
				if ( opinionsAtXY == null ) {
					opinionsAtXY = new TreeMap<Long, TreeMap<Long, Double> >();
					opinions.put( new ConstantPair<Long, Long>( x, y ), new TreeMap<Long, TreeMap<Long, Double> >() );
				}
				
				
				TreeMap<Long, Double> opinion = opinionsAtXY.get( currPos );
				
				if (opinion == null ) {
					opinion = new TreeMap<Long, Double>();
					opinionsAtXY.put( currPos, opinion );
				}
				
				
				try {
					
					double sum = 1.0;
					
//					for ( int j = 0; j < measurements.length; ++j ) {
//						sum += Math.abs( this.func.value( coordinates[j], this.initialGuess) - measurements[j] );
//					}
//					
//					sum /= ( measurements.length - 1 );
					
					double val = this.func.value( coordinates[i], this.initialGuess ) - measurements[i];
					opinion.put( currPos, Math.abs(  val ) / sum );
					currentChange += Math.abs( val );
					
				} catch (FunctionEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException();
				}
				
			}
		}
		

	}

}
