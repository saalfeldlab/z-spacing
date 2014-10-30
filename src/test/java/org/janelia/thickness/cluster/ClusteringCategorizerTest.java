/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.Random;

import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ClusteringCategorizerTest {

	Random rng = new Random( 100 );
	int nSamples = 5000;
	double[] samples = new double[ nSamples ];
	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		for ( int i = 0; i < nSamples; ++i ) {
			samples[i] = rng.nextDouble();
		}
	}

	@Test
	public void test() {
		
		final KMeansPlusPlusClusterer<ZPositionWrapper> clustering1 = new KMeansPlusPlusClusterer<ZPositionWrapper>( 2, 100 );
		final DBSCANClusterer<ZPositionWrapper> clustering2         = new DBSCANClusterer<ZPositionWrapper>( 0.5, 10);
		
		final ClusteringCategorizer<Clusterer<ZPositionWrapper>> c1 = new ClusteringCategorizer<Clusterer<ZPositionWrapper>>( clustering1 );
		final ClusteringCategorizer<Clusterer<ZPositionWrapper>> c2 = new ClusteringCategorizer<Clusterer<ZPositionWrapper>>( clustering2 );
		
		final int[] r1 = c1.getLabels( samples );
		final int[] r2 = c2.getLabels( samples );
		
		Assert.assertEquals( r1.length, nSamples );
		Assert.assertEquals( r2, null );
		
		for ( final int r : r1 )
			Assert.assertTrue( r == 0 || r == 1 );
		
	}

}
