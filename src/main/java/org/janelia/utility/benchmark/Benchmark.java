package org.janelia.utility.benchmark;

import net.imglib2.realtransform.InvertibleRealTransform;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.janelia.utility.realtransform.ScaleAndShift;

/**
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */

public class Benchmark {
	
	private final ExecutionFunctor func;
	private double mean = Double.NaN;
	private double var  = Double.NaN;
	
	/**
	 * @param func
	 */
	public Benchmark(final ExecutionFunctor func) {
		super();
		this.func = func;
	}
	
	/**
	 * @return the mean
	 */
	public double getMean() {
		return mean;
	}


	/**
	 * @return the variance
	 */
	public double getVar() {
		return var;
	}

	public void evaluate( final int nWarmupIterations, final int nRepetitions, final int nIterations ) {
		final double[] list = new double[ nRepetitions ];
		
		long start, end;
		
		for ( int w = 0; w < nWarmupIterations; ++w )
			func.run();
		
		
		for ( int r = 0; r < nRepetitions; ++r ) {
			
			start = System.nanoTime();
			for ( int i = 0; i < nIterations; ++i ) {
				func.run();
			}
			end = System.nanoTime();
			final long tmp1 = System.nanoTime();
			System.nanoTime();
			final long tmp2 = System.nanoTime();
			list[ r ] =  ( end - start - ( tmp2 -tmp1 ) ) * 1.0 / nIterations;
		}
		
		this.mean = new Mean().evaluate( list );
		this.var  = new Variance().evaluate( list, this.mean );
	}
	
	
	public static void main(final String[] args) {
		// compare direct evaluation vs using a functor
		final double[] source = new double[] { 1.0, 1.0 };
		final double[] target = new double[ source.length ];
		final InvertibleRealTransform tf = new ScaleAndShift( new double[] { 1.0, 1.0 }, new double[] { 0.0, 0.0 } );
		final Benchmark benchmark = new Benchmark( new TransformExecutor(source, target, tf) );
		
		final int nWarmupIterations = 1000000;
		final int nRepetitions      = 2000;
		final int nIterations       = 20000;
		
		benchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		
		for ( int w = 0; w < nWarmupIterations; ++w )
			tf.apply(source, target);
		
		final double[] list = new double[ nRepetitions ];
		
		for ( int w = 0; w < nWarmupIterations; ++w )
			tf.apply(source, target);
		
		for ( int r = 0; r < nRepetitions; ++r ) {
			final long start = System.nanoTime();
			for ( int i = 0; i < nIterations; ++i ) {
				tf.apply(source, target);
			}
			final long end = System.nanoTime();
			list[ r ] = ( end - start ) * 1.0 / nIterations;
		}
		
		final double mean = new Mean().evaluate( list );
		final double var  = new Variance().evaluate( list, mean );
		
		System.out.println( String.format( "\t\tmean\t\t\tstd\nvirtual\t\t%.04f\t\t\t%.04f\nexplicit\t%.04f\t\t\t%.04f", 
				benchmark.getMean(), Math.sqrt( benchmark.getVar() ), mean, Math.sqrt( var ) ) );
		
	}

}
