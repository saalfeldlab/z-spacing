/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

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
		final double[] distance = new double[ coordinates.length ];
		for (int i = 0; i < distance.length-1; i++) {
			distance[i] = coordinates[i+1] - coordinates[i];
		}
		distance[distance.length-1] = distance[distance.length-2];
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


	@Override
	public <U extends RealType<U>> double[][] getLabels(
			final RandomAccessibleInterval<U> strip) {
		// TODO Auto-generated method stub
		return null;
	}

}
