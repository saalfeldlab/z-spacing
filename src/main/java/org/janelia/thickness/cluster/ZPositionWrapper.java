/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ZPositionWrapper implements Clusterable {

	
	private final double[] zPosition;
	
	
	/**
	 * @param zPosition
	 */
	public ZPositionWrapper(final double[] zPosition) {
		super();
		if ( zPosition.length != 1 ) {
			throw new RuntimeException( "ZPositionWrapper: zPosition must have length 1!" );
		}
		this.zPosition = zPosition;
	}
	
	
	/**
	 * @param zPosition
	 */
	public ZPositionWrapper(final double zPosition) {
		this( new double[] { zPosition } );
	}


	@Override
	public double[] getPoint() {
		return this.zPosition;
	}
	
	
	public static List< ZPositionWrapper > toList( final double[] coordinates ) {
		final ArrayList<ZPositionWrapper> list = new ArrayList<ZPositionWrapper>();
		for ( final double c : coordinates )
			list.add( new ZPositionWrapper( c ) );
		return list;
	}
	
	

}
