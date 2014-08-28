package org.janelia.utility.realtransform;

import java.util.Arrays;

import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Translation;

import org.janelia.utility.benchmark.Benchmark;
import org.janelia.utility.benchmark.InverseTransformExecutor;
import org.janelia.utility.benchmark.TransformExecutor;


public class ScaleAndShiftBenchmark {
	
	public static InvertibleRealTransform createAffine( final int nDimensions ) {
		final AffineTransform affine = new AffineTransform(nDimensions);
		for ( int i = 0; i < nDimensions; ++i ) {
			affine.set( 1.0, i, i );
		}
		return affine;
	}
	
	public static InvertibleRealTransform createScaleAndShift( final int nDimensions ) {
		final double[] shifts = new double[ nDimensions ];
		final double[] scales = new double[ nDimensions ];
		for (int i = 0; i < scales.length; i++) {
			shifts[ i ] = 0.0;
			scales[ i ] = 1.0;
		}
		return new ScaleAndShift(scales, shifts);
	}
	
	public static InvertibleRealTransform createChainedTransform( final int nDimensions ) {
		final double[] shifts = new double[ nDimensions ];
		final double[] scales = new double[ nDimensions ];
		for (int i = 0; i < scales.length; i++) {
			shifts[ i ] = 0.0;
			scales[ i ] = 1.0;
		}
		final Scale scale       = new Scale( scales );
		final Translation shift = new Translation( shifts );
		final InvertibleRealTransformSequence sequence = new InvertibleRealTransformSequence();
		sequence.add( scale );
		sequence.add( shift );
		return sequence;
	}

	
	public static double[] collectRuntimesForward( final InvertibleRealTransform tf,
			final double[] source,
			final double[] target,
			final int nWarmupIterations,  
			final int nRepetitions,
			final int nIterations ) {
		final double[] list = new double[ nRepetitions ];
		
		for ( int w = 0; w < nRepetitions; ++w ) {
			tf.apply( source, target );
		}
		
		for ( int r = 0; r < nRepetitions; ++r ) {
			final long start = System.nanoTime();
			for ( int i = 0; i < nIterations; ++i ) {
				tf.apply( source, target );
			}
			final long end = System.nanoTime();
			list[ r ] =  ( end - start ) / nIterations;
		}
		return list;
	}
	
	
	public static double[] collectRuntimesInverse( final InvertibleRealTransform tf,
			final double[] source,
			final double[] target,
			final int nWarmupIterations,  
			final int nRepetitions,
			final int nIterations) {
		final double[] list = new double[ nRepetitions ];
		
		for ( int w = 0; w < nRepetitions; ++w ) {
			tf.applyInverse( source, target );
		}
		
		for ( int r = 0; r < nRepetitions; ++r ) {
			final long start = System.nanoTime();
			for ( int i = 0; i < nIterations; ++i ) {
				tf.applyInverse( source, target );
			}
			final long end = System.nanoTime();
			list[ r ] =  ( end - start ) / nIterations;
		}
		return list;
	}
	
	
	public static void main(final String[] args) {
		final int nIterations       = Integer.parseInt( args[0] );
		final int nDimensions       = Integer.parseInt( args[1] );
		final int nWarmupIterations = Integer.parseInt( args[2] );
		final int nRepetitions      = Integer.parseInt( args[3] );
		final boolean useCsv       = ( args.length >= 5 ? args[4] : "" ).toLowerCase().equals( "csv" );
		final String separator      = ( args.length >= 6 ? args[5] : "," );
		final boolean withHeader   = ( args.length >= 7 ? args[6] : "" ).toLowerCase().equals( "header" );
		final String[] separators   = new String[ 14 ];
	
		final InvertibleRealTransform affine = createAffine(nDimensions);
		final InvertibleRealTransform sas    = createScaleAndShift(nDimensions);
		final InvertibleRealTransform chain  = createChainedTransform(nDimensions);
		
		final double[] affineResult = new double[ nDimensions ];
		final double[] sasResult    = new double[ nDimensions ];
		final double[] chainResult  = new double[ nDimensions ];
		final double[] sourcePoint  = new double[ nDimensions ];
		final double[] targetPoint  = new double[ nDimensions ];
		
		for (int i = 0; i < sourcePoint.length; i++) {
			sourcePoint[ i ] = 1.0;
			targetPoint[ i ] = 1.0;
		}
		Arrays.fill( separators, separator );
		String resultString = "";
		if ( useCsv && withHeader )
			resultString += String.format( "dimensions%s"
					+ "iterations%s"
					+ "repetitions%s"
					+ "affine%s"
					+ "affine-std%s"
					+ "sas%s"
					+ "sas-std%s"
					+ "chain%s"
					+ "chain-std%s"
					+ "affine-inverse%s"
					+ "affine-inverse-std%s"
					+ "sas-inverse%s"
					+ "sas-inverse-std%s"
					+ "chain-inverse%s"
					+ "chain-inverse-std\n",
					separator, separator, separator, separator, separator, separator, separator,
					separator, separator, separator, separator, separator, separator, separator );
		
		if ( useCsv ) 
			resultString += String.format( "%d%s%d%s%d%s", nDimensions, separator, nIterations, separator, nRepetitions, separator );
		
		// forward transforms
		// affine
		final Benchmark affineBenchmark = new Benchmark( new TransformExecutor( sourcePoint, affineResult, affine ) );
		affineBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double affineMean = affineBenchmark.getMean();
		final double affineVar  = affineBenchmark.getVar();
		final double affineStd  = Math.sqrt( affineVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f%s", affineMean, separator, affineStd, separator );
		else
			resultString += "affine:\t\t" + affineMean + " (" + affineStd + ")\n";
		
		// scale and shift
		final Benchmark sasBenchmark = new Benchmark( new TransformExecutor( sourcePoint, sasResult, sas) );
		sasBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double sasMean = sasBenchmark.getMean();
		final double sasVar  = sasBenchmark.getVar();
		final double sasStd  = Math.sqrt( sasVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f%s", sasMean, separator, sasStd, separator );
		else
			resultString += "sas:\t\t" + sasMean + " (" + sasStd + ")\n";
		
		// chain of scale and translation
		final Benchmark chainBenchmark = new Benchmark( new TransformExecutor( sourcePoint, chainResult, chain) );
		chainBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double chainMean = chainBenchmark.getMean();
		final double chainVar  = chainBenchmark.getVar();
		final double chainStd  = Math.sqrt( chainVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f%s", chainMean, separator, chainStd, separator );
		else
			resultString += "chain:\t\t" + chainMean + " (" + chainStd + ")\n";
		
		
		// inverse transforms
		// affine
		final Benchmark affineInverseBenchmark = new Benchmark( new InverseTransformExecutor( affineResult, sourcePoint, affine ) );
		affineInverseBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double affineInverseMean = affineInverseBenchmark.getMean();
		final double affineInverseVar  = affineInverseBenchmark.getVar();
		final double affineInverseStd  = Math.sqrt( affineInverseVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f%s", affineInverseMean, separator, affineInverseStd, separator );
		else
			resultString += "affine-inverse:\t\t" + affineInverseMean + " (" + affineInverseStd + ")\n";
		
		// scale and shift
		final Benchmark sasInverseBenchmark = new Benchmark( new InverseTransformExecutor( sasResult, sourcePoint, sas ) );
		sasInverseBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double sasInverseMean = sasInverseBenchmark.getMean();
		final double sasInverseVar  = sasInverseBenchmark.getVar();
		final double sasInverseStd  = Math.sqrt( sasInverseVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f%s", sasInverseMean, separator, sasInverseStd, separator );
		else
			resultString += "sas-inverse:\t\t" + sasInverseMean + " (" + sasInverseStd + ")\n";
		
		// chain of scale and translation
		final Benchmark chainInverseBenchmark = new Benchmark( new InverseTransformExecutor( chainResult, sourcePoint, chain ) );
		chainInverseBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		final double chainInverseMean = chainInverseBenchmark.getMean();
		final double chainInverseVar  = chainInverseBenchmark.getVar();
		final double chainInverseStd  = Math.sqrt( chainInverseVar );
		if ( useCsv )
			resultString += String.format( "%f%s%f", chainInverseMean, separator, chainInverseStd );
		else
			resultString += "chain-inverse:\t\t" + chainInverseMean + " (" + chainInverseStd + ")";
		
		System.out.println( resultString );
	}

}
