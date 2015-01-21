/**
 * 
 */
package org.janelia.thickness.inference;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class Options implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3591334824905556420L;


	public static Options generateDefaultOptions() {
	        final Options result = new Options();
	        result.multiplierGenerationRegularizerWeight = 0.01;
	        result.coordinateUpdateRegularizerWeight = 0.01;
	        result.shiftProportion = 0.5;
	        result.nIterations = 1;
	        result.nThreads = 1;
	        result.comparisonRange = 10;
	        result.neighborRegularizerWeight = 0.05;
	        result.minimumSectionThickness = 0.1;
	        result.windowRange = 150;
	        result.shiftsSmoothingSigma = 4.0;
	        result.shiftsSmoothingRange = 10;
	        result.withRegularization = true;
	        result.multiplierRegularizerDecaySpeed = 10000.0;
	        result.multiplierWeightsSigma = 0.2;
	        result.multiplierEstimationIterations = 10;
	        result.withReorder = true;
	        return result;
	}
	
	public Double multiplierGenerationRegularizerWeight; // m_regularized = m * ( 1 - w ) + 1 * w
	public Double coordinateUpdateRegularizerWeight; // coordinate_regularized = predicted * ( 1 - w ) + original * w
	public Double shiftProportion; // actual_shift = shift * shiftProportion
	public Integer nIterations; // number of iterations
	public Integer nThreads; // number of threads
	public Integer comparisonRange; // range for cross correlations
	public Double neighborRegularizerWeight;
	public Double minimumSectionThickness;
	public Integer windowRange;
	public Double shiftsSmoothingSigma;
	public Integer shiftsSmoothingRange;
	public Boolean withRegularization;
	public Double multiplierRegularizerDecaySpeed;
	public Double multiplierWeightsSigma;
	public Integer multiplierEstimationIterations;
	public Boolean withReorder;
	
	public static Options read( final String filename ) {
		final String defaultString = String.format( "Options.read( \"%s\" )", filename );
		final Options result = Options.generateDefaultOptions();
		try {
			final BufferedReader br = new BufferedReader(new FileReader( filename ) );
			try {
				String line = br.readLine();
				while ( line != null ) {
					final String[] option = line.split( "\\s+" );
					if ( option.length != 2 ) {
						System.err.println( String.format( "%s: ignoring \"%s\" (not a valid option line).", defaultString, line ) );
						line = br.readLine();
						continue;
					}
					
					try {
						final Field f = result.getClass().getDeclaredField( option[0] );
						final Object var = f.get( result );
						if ( var instanceof Double )
							f.set( result, Double.valueOf( option[1] ).doubleValue() );
						else if ( var instanceof Integer )
							f.set( result, Integer.valueOf( option[1] ).intValue() );
						else if ( var instanceof Boolean )
							f.set( result, Boolean.valueOf( option[1] ).booleanValue() );
						else
							System.err.println( String.format( "%s: ignoring \"%s\" (%s not a valid type (%s) ).", defaultString, line, option[0], var.getClass().toString() ) );
					} catch (final IllegalArgumentException e) {
						System.err.println( String.format( "%s: ignoring \"%s\" (cannot set %s to %s)", defaultString, line, option[0], option[1] ) );
					} catch (final IllegalAccessException e) {
						System.err.println( String.format( "%s: ignoring \"%s\" (cannot access Options object)", defaultString, line ) );
						e.printStackTrace();
					} catch (final NoSuchFieldException e) {
						System.err.println( String.format( "%s: ignoring \"%s\" (not a valid field).", defaultString, line ) );
					} catch (final SecurityException e) {
						System.err.println( String.format( "%s: ignoring \"%s\" (SecurityException).", defaultString, line ) );
						e.printStackTrace();
					} finally {
						line = br.readLine();
					}
				}
			} catch (final IOException e) {
				System.err.println( String.format( "%s: Could not read line.", defaultString ) );
				e.printStackTrace();
			}
		} catch (final FileNotFoundException e) {
			System.err.println( String.format( "%s: File not found - return default options.", defaultString ) );
			return result;
		}  
		
		return result;
	}
	
	
	@Override
	public Options clone() {
		final Options result = new Options();
		for ( final Field f : this.getClass().getDeclaredFields() ) {
			try {
				f.set( result, f.get( this ) );
			} catch (final IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
	    sb.append("[");
	    sb.append(getClass().getName());
	    sb.append("]\n");
	    for ( final Field f : this.getClass().getDeclaredFields() ) {
	  	  sb.append( f.getName() );
	  	  sb.append( "\t" );
	  	  try {
				sb.append( f.get( this ) );
			} catch (final IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  	  sb.append( "\n" );
	    }
	    
	    return sb.toString();
	}
	
	
	public void toFile( final String filename ) throws FileNotFoundException {
		final String optionString = this.toString();
		final PrintWriter outFile = new PrintWriter( filename );
		outFile.println( optionString );
		outFile.close();
	}
	
	
	@Override
	public boolean equals( final Object other ) {
		if ( other instanceof Options ) {
			for ( final Field f : this.getClass().getDeclaredFields() ) {
	    		try {
					if ( ! f.get( this ).equals( f.get( other ) ) ) {
						return false;
					}
				} catch (final IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				} catch (final IllegalAccessException e) {
					// TODO Auto-generated catch block
						e.printStackTrace();
						return false;
					}
	        	}
	    		return true;
	    	} else
	    		return false;
    }
	
	
	public static void main(final String[] args) {
		final String fn = "/data/hanslovskyp/khaled_2014_10_24/range=5_2014-10-27 11:22:17.651999/options";
		final Options options = Options.read( fn );
		System.out.println( options.toString() );
		System.out.println( Options.generateDefaultOptions().toString() );
	}
	
}