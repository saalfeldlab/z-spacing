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
import org.janelia.thickness.cluster.evaluation.ClusterQualityEvaluation;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ChooseBestClusteringCategorizer implements Categorizer {

	
	private final ClusterQualityEvaluation eval;
	private final List< Clusterer< ZPositionWrapper > > clusterers;	
	
	/**
	 * @param numbersOfClusters
	 */
	public ChooseBestClusteringCategorizer(
			final List< Clusterer< ZPositionWrapper > > clusterers,
			final ClusterQualityEvaluation eval ) {
		super();
		this.clusterers = clusterers;
		this.eval = eval;
	}


	@Override
	public double[][] getLabels(final double[] coordinates) {
		final double[] distance = new double[ coordinates.length ];
		for (int i = 0; i < distance.length-1; i++) {
			distance[i] = coordinates[i+1] - coordinates[i];
		}
		distance[distance.length-1] = distance[distance.length-2];
		
		final List<ZPositionWrapper> list = ZPositionWrapper.toList( distance );
		
		List<? extends Cluster<ZPositionWrapper>> currentBest = null;
		double currentBestScore = -Double.MAX_VALUE;
		int currentBestIndex = -1;
		
		for ( int i = 0; i < clusterers.size(); ++i ) {
			final Clusterer<ZPositionWrapper> c = clusterers.get( i ); 
			final List<? extends Cluster<ZPositionWrapper>> result = c.cluster( list );
			if ( result.get( 0 ) instanceof CentroidCluster ) {
				@SuppressWarnings("unchecked") // dirty cast necessary here. would not be necessary if apache math commons would distinguish appropriately
				final double score = this.eval.evaluate( (List<CentroidCluster< ZPositionWrapper > >) result );
				if ( score > currentBestScore ) {
					currentBest = result;
					currentBestScore = score;
					currentBestIndex = i;
				}
			}
		}
		
		final double[][] softLabels;
		if ( currentBest != null && (currentBest.get(0) instanceof CentroidCluster<?> ) ) {
			
			final ArrayList<CentroidCluster<ZPositionWrapper>> centroids = new ArrayList< CentroidCluster< ZPositionWrapper > >();
			for ( final Cluster<ZPositionWrapper> r : currentBest )
				centroids.add( (CentroidCluster< ZPositionWrapper >) r );
		
			softLabels = Convenience.getSoftAssignments( list, centroids, clusterers.get( currentBestIndex ).getDistanceMeasure() );
		}
		else
			softLabels = null;
		
		IJ.log( "Chose number of clusters == " + softLabels[0].length );
		
		return softLabels;
	}

}
