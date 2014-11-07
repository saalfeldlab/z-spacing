/**
 * 
 */
package org.janelia.thickness.cluster.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.janelia.thickness.cluster.ZPositionWrapper;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CalinskiHarabasz implements ClusterQualityEvaluation {

	@Override
	public double evaluate( final List<? extends CentroidCluster<ZPositionWrapper>> centers ) {
		
		double dataMean = 0.0;
		double intraClusterDifference = 0.0;
		
		int count = 0;
		for ( final CentroidCluster<ZPositionWrapper> c : centers ) {
			for ( final ZPositionWrapper p : c.getPoints()) {
				final double val = p.getPoint()[0];
				dataMean += val;
				++count;
				
				final double diff = val - c.getCenter().getPoint()[0];
				intraClusterDifference += diff*diff; 
				
			}
		}
		
		dataMean /= count;
		double interClusterDifference = 0.0;
		for ( final CentroidCluster<ZPositionWrapper> c : centers ) {
			final double diff = c.getCenter().getPoint()[0] - dataMean;
			interClusterDifference += c.getPoints().size()*diff*diff;
		}
		
		
		return Math.abs( ( interClusterDifference / centers.size() - 1 ) / ( intraClusterDifference / count - centers.size() ) );
	}
	
	public static void main(final String[] args) {
		
		final double[] points = new double[] { 1.0, 1.5, 10.0, 11.5 };
		final ArrayList<ZPositionWrapper> al = new ArrayList< ZPositionWrapper >();
		for ( final double p : points )
			al.add( new ZPositionWrapper( p ) );
		for ( int i = 1; i <= 4; ++i ) {
			final KMeansPlusPlusClusterer<ZPositionWrapper> kmeans = new KMeansPlusPlusClusterer< ZPositionWrapper >( i, 100 );
			final List<CentroidCluster<ZPositionWrapper>> result = kmeans.cluster( al );
			final double score = new CalinskiHarabasz().evaluate( result );
			System.out.println( i + " " + score );
		}
		
	}

}
