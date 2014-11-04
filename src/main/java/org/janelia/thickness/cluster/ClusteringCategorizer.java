/**
 * 
 */
package org.janelia.thickness.cluster;

import ij.IJ;

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


	@Override
	public double[][] getLabels(final double[] coordinates) {
		IJ.log( "before calculating distances" );
		final double[] distance = new double[ coordinates.length ];
		for (int i = 0; i < distance.length-1; i++) {
			distance[i] = coordinates[i+1] - coordinates[i];
		}
		distance[distance.length-1] = distance[distance.length-2];
		IJ.log( "after calculating distances" );
		final List<ZPositionWrapper> list = ZPositionWrapper.toList( distance );
		final List<? extends Cluster<ZPositionWrapper>> result = this.clusterer.cluster( list );
		if ( (result.get(0) instanceof CentroidCluster<?> ) ) {
			final ArrayList<CentroidCluster<ZPositionWrapper>> centroids = new ArrayList< CentroidCluster< ZPositionWrapper > >();
			for ( final Cluster<ZPositionWrapper> r : result )
				centroids.add( (CentroidCluster< ZPositionWrapper >) r );
			return Convenience.getSoftAssignments( distance, centroids, clusterer.getDistanceMeasure() );
		} else
			return null;
		
		
	}

}
