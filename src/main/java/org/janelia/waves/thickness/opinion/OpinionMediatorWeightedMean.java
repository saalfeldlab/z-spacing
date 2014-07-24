package org.janelia.waves.thickness.opinion;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;

public class OpinionMediatorWeightedMean implements OpinionMediatorInterface {
	
	public enum TYPE {
		ARITHMETIC,
		GEOMETRIC,
		HARMONIC
	}
	
	public interface MeanCalculator {
		Double calculate( ArrayList<Double> data, ArrayList<Double> weights );
	}
	
	public static class Arithmetic implements MeanCalculator {

		public Double calculate(ArrayList<Double> data, ArrayList<Double> weights ) {
			Double res       = 0.0;
			Double weightSum = 0.0; 
			for ( int i = 0; i < data.size(); ++i ) {
				res       += data.get(i) * weights.get(i);
				weightSum += weights.get(i);
			}
			return res / weightSum;
		}
		
	}
	
	public static class Geometric implements MeanCalculator {

		public Double calculate(ArrayList<Double> data, ArrayList<Double> weights ) {
			Double res       = 1.0;
			Double weightSum = 0.0; 
			for ( int i = 0; i < data.size(); ++i ) {
				res       *= Math.pow( data.get(i), weights.get(i) );
				weightSum += weights.get(i);
			}
			return Math.pow( res, 1 / weightSum );
		}
		
	}
	
	public static class Harmonic implements MeanCalculator {

		public Double calculate(ArrayList<Double> data, ArrayList<Double> weights) {
			Double res       = 0.0;
			Double weightSum = 0.0; 
			for ( int i = 0; i < data.size(); ++i ) {
				res       += weights.get(i) / data.get(i);
				weightSum += weights.get(i);
			}
			return weightSum / res;
		}
		
	}
	
	private final TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair< ArrayList<Double>, ArrayList<Double>> > opinionMap;
	private final MeanCalculator calculator;
	private final TreeMap<ConstantTriple<Long, Long, Long>, Double> shiftMap;

	public OpinionMediatorWeightedMean() {
		this( new TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<ArrayList<Double>, ArrayList<Double>>>(), new Arithmetic() );
	}
	
	public OpinionMediatorWeightedMean( MeanCalculator calculator ) {
		this( new TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<ArrayList<Double>, ArrayList<Double>>>(), calculator );
	}

	public OpinionMediatorWeightedMean(
			TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair< ArrayList<Double>, ArrayList<Double>> > opinionMap,
			MeanCalculator calculator) {
		super();
		this.opinionMap = opinionMap;
		this.calculator = calculator;
		this.shiftMap   = new TreeMap<ConstantTriple<Long, Long, Long>, Double>();
		
		for ( Entry<ConstantTriple<Long, Long, Long>, ConstantPair<ArrayList<Double>, ArrayList<Double>>> entry : opinionMap.entrySet() ) {
			this.shiftMap.put( entry.getKey(), this.calculator.calculate( entry.getValue().getA(),  entry.getValue().getB() ) );
		}
	}

	public void clearOpinions() {
		this.opinionMap.clear();
		this.shiftMap.clear();
	}

	public void addOpinions(long x, long y, long zMin, long zMax,
			long zReference, double[] opinions, double[] weights)
			throws InconsistencyError {
		
		if ( zMax <= zMin ) {
			throw new InconsistencyError();
		}
		
		if ( zReference < zMin || zReference > zMax ) {
			throw new InconsistencyError();
		}
		
		if ( opinions.length != zMax - zMin ) {
			throw new InconsistencyError();
		}
		
		if ( weights.length != opinions.length ) {
			throw new InconsistencyError();
		}
		
		
		for ( int i = 0; i < opinions.length; ++i ) {
			ConstantTriple<Long, Long, Long> position = new ConstantTriple<Long, Long, Long>(x, y, zMin );
			if ( ! this.opinionMap.containsKey( position ) ) {
				this.opinionMap.put( position, new ConstantPair< ArrayList<Double>, ArrayList<Double> >(new ArrayList<Double>(), new ArrayList<Double>() ) );
			}
			
			ConstantPair<ArrayList<Double>, ArrayList<Double>> opinionsAt = this.opinionMap.get( position );
			
			opinionsAt.getA().add( opinions[i]);
			opinionsAt.getB().add( weights[i] );
			
			++zMin;
		}
		
		
	}

	public void addOpinions(long x, long y, long zMin, long zMax,
			long zReference, double[] opinions) throws InconsistencyError {
		double[] weights = new double[ opinions.length ];
		
		long zReferenceShifted = zReference - zMin;
		
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = 1.0 / ( 1 + Math.abs( i - zReferenceShifted ) );
		}
		addOpinions(x, y, zMin, zMax, zReference, opinions, weights);
	}

	public TreeMap<ConstantTriple<Long, Long, Long>, Double> fit()
			throws InterruptedException {
		return this.fit( 1 );
	}

	public TreeMap<ConstantTriple<Long, Long, Long>, Double> fit(
			int nCores) throws InterruptedException {
		if ( nCores <= 0 ) {
			nCores = Runtime.getRuntime().availableProcessors();
		}
		
		ExecutorService es = Executors.newFixedThreadPool( nCores );
		
		ArrayList< Callable<Void>> callables = new ArrayList<Callable<Void>>();
		
		for ( final Entry<ConstantTriple<Long, Long, Long>, ConstantPair<ArrayList<Double>, ArrayList<Double>>> entry : this.opinionMap.entrySet() ) {
			
			callables.add( new Callable<Void>() {

				public Void call() throws Exception {
					Double mean = calculator.calculate( entry.getValue().getA(), entry.getValue().getB() );
					
					synchronized ( shiftMap ) {
						shiftMap.put( entry.getKey(), mean );
					}
					return null;
				}
				
			});
			
		}
		
		es.invokeAll( callables );
		
		return this.shiftMap;
	}

}
