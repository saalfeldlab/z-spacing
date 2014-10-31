/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterer;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CrispClusteringCategorizer< T extends Clusterer< ZPositionWrapper > > implements Categorizer {
	
	private final T clusterer;

	
	/**
	 * @param clusterer
	 */
	public CrispClusteringCategorizer(final T clusterer) {
		super();
		this.clusterer = clusterer;
	}


	@Override
	public double[][] getLabels(final double[] coordinates) {
		final List<ZPositionWrapper> list = ZPositionWrapper.toList( coordinates );
		final List<? extends Cluster<ZPositionWrapper>> result = this.clusterer.cluster( list );
		if ( (result.get(0) instanceof CentroidCluster<?> ) ) {
			final ArrayList<CentroidCluster<ZPositionWrapper>> centroids = new ArrayList< CentroidCluster< ZPositionWrapper > >();
			for ( final Cluster<ZPositionWrapper> r : result )
				centroids.add( (CentroidCluster< ZPositionWrapper >) r );
			final int[] assignments = Convenience.getAssignments( coordinates, centroids, clusterer.getDistanceMeasure() );
			final double[][] crispAssignments = new double[ assignments.length ][ result.size() ];
			for ( int i = 0; i < assignments.length; ++i ) {
				crispAssignments[ i ][ assignments[ i ] ] = 1.0;
			}
			return crispAssignments; // Convenience.getSoftAssignments( coordinates, centroids, clusterer.getDistanceMeasure() );
		} else
			return null;
		
		
	}

}
