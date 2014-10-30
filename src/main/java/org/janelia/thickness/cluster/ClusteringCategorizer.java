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
public class ClusteringCategorizer< T extends Clusterer< ZPositionWrapper > > implements Categorizer {
	
	private final T clusterer;

	
	/**
	 * @param clusterer
	 */
	public ClusteringCategorizer( final T clusterer ) {
		super();
		this.clusterer = clusterer;
	}


	@SuppressWarnings("unchecked")
	@Override
	public int[] getLabels(final double[] coordinates) {
		final List<ZPositionWrapper> list = ZPositionWrapper.toList( coordinates );
		final List<? extends Cluster<ZPositionWrapper>> result = this.clusterer.cluster( list );
		if ( (result.get(0) instanceof CentroidCluster<?> ) ) {
			final ArrayList<CentroidCluster<ZPositionWrapper>> centroids = new ArrayList< CentroidCluster< ZPositionWrapper > >();
			for ( final Cluster<ZPositionWrapper> r : result )
				centroids.add( (CentroidCluster< ZPositionWrapper >) r );
			return Convenience.getAssignments( coordinates, centroids, clusterer.getDistanceMeasure() );
		} else
			return null;
		
		
	}

}
