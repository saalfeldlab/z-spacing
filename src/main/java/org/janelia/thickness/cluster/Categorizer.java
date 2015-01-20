/**
 * 
 */
package org.janelia.thickness.cluster;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface Categorizer {
	
	public double[][] getLabels( final double[] coordinates );
	
	public < T extends RealType< T > > double[][] getLabels( final RandomAccessibleInterval<T> strip );
	
	public void setState( int n );

}
