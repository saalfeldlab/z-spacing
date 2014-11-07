/**
 * 
 */
package org.janelia.thickness.cluster.evaluation;

import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.janelia.thickness.cluster.ZPositionWrapper;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface ClusterQualityEvaluation {
	
	public double evaluate( List<? extends CentroidCluster<ZPositionWrapper>> centers );

}
