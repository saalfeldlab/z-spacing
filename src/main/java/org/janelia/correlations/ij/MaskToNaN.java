/**
 * 
 */
package org.janelia.correlations.ij;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class MaskToNaN {
	
	public static interface DecisionMaker {
		
		public float decide( Object pixelArray, int index );
		
	}
	
	
	public static interface Opener {
		
		public ImageProcessor openImage( final String format, int z );
		
	}
	
	public static FloatProcessor toNaN( final ImageProcessor ip, final DecisionMaker d ) {
		final int w = ip.getWidth();
		final int h = ip.getHeight();
		final int n = w*h;
		final FloatProcessor fp   = new FloatProcessor( w, h );
		final Object pixelArray   = ip.getPixels();
		final float[] resultArray = (float[]) fp.getPixels();
		for ( int index = 0; index < n; ++index )
			resultArray[ index ] = d.decide( pixelArray, index);
		return fp;
	}
	
	public static void toNaN( final String inputFormat, final String outputFormat, final int n, final int nThreads, final DecisionMaker d, final Opener o ) throws InterruptedException {
		
		final ExecutorService es                  = Executors.newFixedThreadPool( nThreads );
		final ArrayList<Callable<Void>> callables = new ArrayList< Callable< Void > >();
		
		for ( int i = 0; i < n; ++i ) {
			
			final int finalI = i;
			
			callables.add( new Callable< Void > () {

				@Override
				public Void call() throws Exception {
					final ImageProcessor input  = o.openImage( inputFormat, finalI );
					final FloatProcessor output = toNaN( input, d );
					new FileSaver( new ImagePlus( "", output ) ).saveAsTiff( String.format( outputFormat, finalI ) );
					return null;
				}
				
			});
			
			
			es.invokeAll( callables );
		}
	}
		
		
	public static class ColorDecisionMaker implements DecisionMaker {

		@Override
		public float decide(final Object pixelArray, final int index) {
			final int rgb = ( (int[])pixelArray )[index];
			final int r = ( rgb >> 16 ) & 0xff;
			final int g = ( rgb >>  8 ) & 0xff;
			final int b =   rgb         & 0xff;
			return ( r == g && g == b ) ? r : Float.NaN;
		}
	}
	
	
	public static class ColorOpener implements Opener {

		@Override
		public ColorProcessor openImage( final String format, final int z ) {
			final ImagePlus imp = new ImagePlus( String.format( format, z ) );
			return imp.getProcessor().convertToColorProcessor();
		}
		
	}
	
	
	public static class FloatOpener implements Opener {

		@Override
		public FloatProcessor openImage( final String format, final int z ) {
			final ImagePlus imp = new ImagePlus( String.format( format, z ) );
			return imp.getProcessor().convertToFloatProcessor();
		}
		
	}
	
	public static void main(final String[] args) throws InterruptedException {
		
		final String inputFormat  = "/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation/test-in-%d.tif";
		final String outputFormat = "/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation/test-out-%d.tif";

		final int nThreads      = Runtime.getRuntime().availableProcessors();
		final DecisionMaker cdm = new ColorDecisionMaker();
		final ColorOpener o     = new ColorOpener();
		
		final int n = 1;
		
		toNaN(inputFormat, outputFormat, n, nThreads, cdm, o);
		
		new ImageJ();
		
		new ImagePlus( String.format( inputFormat, 0 ) ).show();
		new ImagePlus( String.format( outputFormat, 0 ) ).show();
		
	}

}
