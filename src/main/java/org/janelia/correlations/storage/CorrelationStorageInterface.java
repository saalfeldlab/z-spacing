/**
 * 
 */
package org.janelia.correlations.storage;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface CorrelationStorageInterface<T extends RealType< T > > extends RandomAccessible<T> {
	
	public RandomAccessibleInterval< T > toMatrix( long x, long y );

}
