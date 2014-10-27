/**
 * 
 */
package org.janelia.thickness.inference;

import java.lang.reflect.Field;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class Options {
	
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
	
	public double multiplierGenerationRegularizerWeight; // m_regularized = m * ( 1 - w ) + 1 * w
	public double coordinateUpdateRegularizerWeight; // coordinate_regularized = predicted * ( 1 - w ) + original * w
	public double shiftProportion; // actual_shift = shift * shiftProportion
	public int nIterations; // number of iterations
	public int nThreads; // number of threads
	public int comparisonRange; // range for cross correlations
	public double neighborRegularizerWeight;
	public double minimumSectionThickness;
	public int windowRange;
	public double shiftsSmoothingSigma;
	public int shiftsSmoothingRange;
	public boolean withRegularization;
	public double multiplierRegularizerDecaySpeed;
	public double multiplierWeightsSigma;
	public int multiplierEstimationIterations;
	public boolean withReorder;
	
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
}