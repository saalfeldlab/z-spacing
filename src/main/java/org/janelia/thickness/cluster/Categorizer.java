/**
 * 
 */
package org.janelia.thickness.cluster;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface Categorizer {
	
	public double[][] getLabels( final double[] coordinates );

}
