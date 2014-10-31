/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class Convenience {
	
	public static < T extends Clusterable > int[] getAssignments( final List< T > samples, final List< CentroidCluster< T > > centroids, final DistanceMeasure dm ) {
		final int[] result = new int[ samples.size() ];
		final int K = centroids.size();
		for (int i = 0; i < result.length; i++) {
			final T s = samples.get( i );
			double minDist = Double.MAX_VALUE;
			for ( int k = 0; k < K; ++k ) {
				final CentroidCluster<T> c = centroids.get( k );
				final double currDist = dm.compute( c.getCenter().getPoint(), s.getPoint() );
				if ( currDist < minDist ) {
					result[i] = k;
					minDist = currDist;
				}
			}
		}
		return result;
	}
	
	
	public static < T extends Clusterable > int[] getAssignments( final double[] samples, final List< CentroidCluster< T > > centroids, final DistanceMeasure dm ) {
		final int[] result = new int[ samples.length ];
		final int K = centroids.size();
		final double[] s = new double[ 1 ];
		for (int i = 0; i < result.length; i++) {
			s[0] = samples[i];
			double minDist = Double.MAX_VALUE;
			for ( int k = 0; k < K; ++k ) {
				final CentroidCluster<T> c = centroids.get( k );
				final double currDist = dm.compute( c.getCenter().getPoint(), s );
				if ( currDist < minDist ) {
					result[i] = k;
					minDist = currDist;
				}
			}
		}
		return result;
	}
	
	
	public static < T extends Clusterable > double[][] getSoftAssignments( final List< T > samples, final List< CentroidCluster< T > > centroids, final DistanceMeasure dm ) {
		final int K = centroids.size();
		final double[][] result = new double[ samples.size() ][ K ];
		for (int i = 0; i < result.length; i++) {
			final T s = samples.get( i );
			double distSum = 0.0;
			final double[] resultAt = result[ i ];
			for ( int k = 0; k < K; ++k ) {
				final CentroidCluster<T> c = centroids.get( k );
				final double currDist = dm.compute( c.getCenter().getPoint(), s.getPoint() );
				distSum += currDist;
				resultAt[ k ] = currDist;
			}
			// normalize
			for ( int k = 0; k < K; ++k )
				resultAt[ k ] /= distSum;
		}
		return result;
	}
	
	
	public static < T extends Clusterable > double[][] getSoftAssignments( final double[] samples, final List< CentroidCluster< T > > centroids, final DistanceMeasure dm ) {
		final int K = centroids.size();
		final double[][] result = new double[ samples.length ][ K ];
		final double[] s = new double[ 1 ];
		for (int i = 0; i < result.length; i++) {
			s[0] = samples[i];
			double distSum = 0.0;
			final double[] resultAt = result[ i ];
			for ( int k = 0; k < K; ++k ) {
				final CentroidCluster<T> c = centroids.get( k );
				final double currDist = dm.compute( c.getCenter().getPoint(), s );
				distSum += currDist;
				resultAt[ k ] = currDist;
			}
			// normalize
			for ( int k = 0; k < K; ++k )
				resultAt[ k ] /= distSum;
		}
		return result;
	}

}
