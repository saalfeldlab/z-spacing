/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.ArrayList;
import java.util.List;

import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel2D;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class FilterRansac implements Categorizer {
	
	private final static float[] ZERO_1D = new float[] { 0.0f };
	
	private final int iterations;
	private final float maxEpsilon;
	private final float minInlierRatio;
	private final int minNumInliers;
	private final float maxTrust;

	
	/**
	 * @param iterations
	 * @param maxEpsilon
	 * @param minInlierRatio
	 * @param minNumInliers
	 * @param maxTrust
	 */
	public FilterRansac(final int iterations, final float maxEpsilon, final float minInlierRatio,
			final int minNumInliers, final float maxTrust) {
		super();
		this.iterations = iterations;
		this.maxEpsilon = maxEpsilon;
		this.minInlierRatio = minInlierRatio;
		this.minNumInliers = minNumInliers;
		this.maxTrust = maxTrust;
	}
	
	
	public FilterRansac() {
		this.iterations     = 10;
		this.maxEpsilon     = 1.0f;
		this.minInlierRatio = 0.1f;
		this.minNumInliers  = 10;
		this.maxTrust       = 0.5f;
	}


	@Override
	public double[][] getLabels(final double[] coordinates) {
		final double[] distance = new double[ coordinates.length ];
		for (int i = 0; i < distance.length-1; i++) {
			distance[i] = coordinates[i+1] - coordinates[i];
		}
		distance[distance.length-1] = distance[distance.length-2];
		
		final TranslationModel2D model = new TranslationModel2D();
		final ArrayList<PointMatch> candidates = new ArrayList< PointMatch >();
		final float[] ZERO_IN_FIRST_DIMENSION_2D = new float[] { 0.0f, 0.0f };
		double d;
		for (int i = 0; i < distance.length; i++) {
			d = distance[i];
			ZERO_IN_FIRST_DIMENSION_2D[1] = i;
			candidates.add( new PointMatch( new Point( ZERO_IN_FIRST_DIMENSION_2D ), new Point( new float[] { (float)d, i } ) ) );
		}
		boolean foundInliers = true;
		final List< List< PointMatch > > clusters = new ArrayList< List < PointMatch > >();
		while( foundInliers ) {
			final ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();
				try {
					foundInliers = model.filterRansac(candidates, inliers, iterations, maxEpsilon, minInlierRatio, minNumInliers, maxTrust);
					candidates.removeAll( inliers );
					clusters.add( inliers );
				} catch (final NotEnoughDataPointsException e) {
					foundInliers = false;
				}
		}
		
		final double[][] result;
		
		if ( clusters.size() > 0 ) {
			final int nClusters = clusters.size();
			final int nClasses; 
			if ( candidates.size() < 0 )
				nClasses = nClusters + 1;
			else
				nClasses = nClusters;
			result = new double[ coordinates.length ][ nClasses ];
			for ( int i = 0; i < nClusters; ++i ) {
				final List<PointMatch> c = clusters.get( i );
				final int nEntries = c.size();
				for ( int k = 0; k < nEntries; ++k ) {
					final int index = (int)c.get( k ).getP2().getW()[1];
					result[ index ][ i ] = 1.0;
				}
			}
			
			final int nEntries = candidates.size();
			for ( int k = 0; k < nEntries; ++k ) {
				final int index = (int)candidates.get( k ).getP2().getW()[1];
				result[ index ][ nClusters ] = 1.0;
			}
			
		}
		else
			result = new double[ coordinates.length ][ 1 ];
		
		return result;
	}

}
