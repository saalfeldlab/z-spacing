package org.janelia.utility.realtransform;

import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Translation;


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
	
	
	public static void main(final String[] args) {
		final int nIterations       = Integer.parseInt( args[0] );
		final int nDimensions       = Integer.parseInt( args[1] );
		final int nWarmupIterations = Integer.parseInt( args[2] );
		long start = 0;
		long end   = 0;
	
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
		
		
		// forward transforms
		// affine
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			affine.apply( sourcePoint, affineResult );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			affine.apply( sourcePoint, affineResult );
		}
		end = System.nanoTime();
		System.out.println("affine:\t\t" + (end-start)*1.0/nIterations );
		
		
		// scale and shift
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			sas.apply( sourcePoint, sasResult );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			sas.apply( sourcePoint, sasResult );
		}
		end = System.nanoTime();
		System.out.println("sas:\t\t" + (end-start)*1.0/nIterations );
		
		
		// chain of scale and translation
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			chain.apply( sourcePoint, chainResult );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			chain.apply( sourcePoint, chainResult );
		}
		end = System.nanoTime();
		System.out.println("chain:\t\t" + (end-start)*1.0/nIterations );
		
		
		// inverse transforms
		// affine
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			affine.applyInverse( affineResult, targetPoint );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			affine.applyInverse( affineResult, targetPoint );
		}
		end = System.nanoTime();
		System.out.println("affine (inverse):\t" + (end-start)*1.0/nIterations );
		
		
		// scale and shift
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			sas.applyInverse( sasResult, targetPoint );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			sas.applyInverse( sasResult, targetPoint );
		}
		end = System.nanoTime();
		System.out.println("sas (inverse):\t\t" + (end-start)*1.0/nIterations );
		
		
		// chain of scale and translation
		for ( int iteration = 0; iteration < nWarmupIterations; ++iteration ) {
			chain.applyInverse( chainResult, targetPoint );
		}
		
		start = System.nanoTime();
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			chain.applyInverse( chainResult, targetPoint );
		}
		end = System.nanoTime();
		System.out.println("chain (inverse):\t" + (end-start)*1.0/nIterations );
		
	
	}

}
