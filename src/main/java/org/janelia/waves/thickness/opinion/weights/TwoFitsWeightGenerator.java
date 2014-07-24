package org.janelia.waves.thickness.opinion.weights;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

@SuppressWarnings("deprecation")
public class TwoFitsWeightGenerator extends FitWeightGenerator {
	
	private final ArrayList< Integer > ignoreBins;
	
	



	public TwoFitsWeightGenerator(CurveFitter fitter,
			SymmetricDifferentiableParameterizedFunction func,
			double[] initialGuess, ArrayList<Integer> ignoreBins) {
		super(fitter, func, initialGuess);
		this.ignoreBins = ignoreBins;
	}

	public void generate(double[] coordinates, double[] measurements, long x, long y,
			double zRef, long zMin, long zMax, long zPosition) {
		
			this.initialGuess[0] = zRef;
		
		
			this.fitter.clearObservations();
			
			double[] weights = new double[measurements.length];
			
			for (int i = 0; i < measurements.length; ++i ) {
				weights[i] = this.getWeightFor( x, y, zMin + i);
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
			
			this.fitter.clearObservations();
			
			int zeroCount = 0;
			
			for ( int i = 0; i < weights.length; ++i ) {
				if ( this.ignoreBins.contains( (int) ( zMin - zPosition + i ) ) ) {
					weights[i] = 0;
					++zeroCount;
				}
				this.fitter.addObservedPoint( weights[i], coordinates[i], measurements[i]);
			}
			
			double[] secondGuess = initialGuess;
			
			if ( weights.length - zeroCount >= initialGuess.length ) {
			
				try {
					secondGuess = this.fitter.fit( this.func, this.initialGuess );
				} catch (OptimizationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					throw new RuntimeException();
				} catch (FunctionEvaluationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			
		
			
			
			TreeMap<Long, TreeMap<Long, Double> > opinionsAtXY = opinions.get( new ConstantPair<Long, Long>( x, y ) );
			
			if ( opinionsAtXY == null ) {
				opinionsAtXY = new TreeMap<Long, TreeMap<Long, Double> >();
				opinions.put( new ConstantPair<Long, Long>( x, y ), new TreeMap<Long, TreeMap<Long, Double> >() );
			}
			
			for ( Integer ignore : this.ignoreBins ) {
				
				long currPos         = zPosition + ignore;
				int  currPosCentered = (int) (currPos - zMin);
				
				if ( currPosCentered < 0 || currPosCentered >= measurements.length ) {
					continue;
				}
				
			
			
				TreeMap<Long, Double> opinion = opinionsAtXY.get( currPos );
				
				if (opinion == null ) {
					opinion = new TreeMap<Long, Double>();
					opinionsAtXY.put( currPos, opinion );
				}
				
				try {
					double initialValue = this.func.value( coordinates[currPosCentered], this.initialGuess);
					double secondValue  = this.func.value( coordinates[currPosCentered], secondGuess );
					opinion.put( currPos, Math.abs( 2.0 * ( initialValue - secondValue ) / ( initialValue + secondValue ) ) );
				} catch (FunctionEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
		}

}
