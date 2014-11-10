/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.ArrayList;
import java.util.List;

import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel1D;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.janelia.models.TranslationModelND;

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
		// take default values from TrakEM2: pom-trakem2/TrakEM2_/src/main/java/mpicbg/trakem2/align/Align.java
		// how to calculate iterations (Saalfeld thesis, p. 27)
		// probability that each randomly selected minimal sample contains at least one outlier:
		// p = (1-r^m)^k
		// with r := expected ratio of inliers
		//      m := minimal number of samples to estimate a hypotheses
		//      k := number of iterations
		// ->   k  = ln(p)/ln(1-r^m)
		// p shouldn't be too large
		// p = 0.1?
		// m = 1
		// r = 0.5?
		this(
		10,   // iterations
		0.1f,   // maxEpsilon - maximum allowed error (in euclidian distance)
		0.1f,   // minInlierRatio
		7,      // minNumInliers
		3f      // maxTrust - number of sigmas to be included
		);
	}


	@Override
	public double[][] getLabels(final double[] coordinates) {
		final double[] distance = new double[ coordinates.length ];
		for (int i = 0; i < distance.length-1; i++) {
			distance[i] = coordinates[i+1] - coordinates[i];
		}
		distance[distance.length-1] = distance[distance.length-2];
		
		final TranslationModel1D model = new TranslationModel1D();
		final ArrayList<PointMatch> candidates = new ArrayList< PointMatch >();
		double d;
		for (int i = 0; i < distance.length; i++) {
			d = distance[i];
			final float[] ZERO_IN_FIRST_DIMENSION_2D = new float[] { 0.0f, 0.0f };
			ZERO_IN_FIRST_DIMENSION_2D[1] = i;
			candidates.add( new PointMatch( new Point( ZERO_IN_FIRST_DIMENSION_2D ), new Point( new float[] { (float)d, i } ) ) );
		}
		
		final List<List<PointMatch>> clusters = findClusters( model, candidates, iterations, maxEpsilon, minInlierRatio, minNumInliers, maxTrust );
		
		final double[][] result;
		
		if ( clusters.size() > 0 ) {
			result = generateResult( clusters, candidates, coordinates.length ); 
		}
		else
			result = new double[ coordinates.length ][ 1 ];
//		IJ.log( "OCCCK " + result[0].length );
		return result;
	}


	@Override
	public <T extends RealType<T>> double[][] getLabels(
			final RandomAccessibleInterval<T> strip ) {
		final int numberOfCorrelations = (int) strip.dimension( 0 );
		final int numberOfZPositions   = (int) strip.dimension( 1 );
		
		final float[] NDIMENSIONS_ZERO = new float[ numberOfCorrelations ];
		
		final ArrayList<PointMatch> candidates = new ArrayList< PointMatch>();
		for ( int i = 0; i < numberOfZPositions; ++i ) {
			final Cursor<T> cursor = Views.flatIterable( Views.hyperSlice( strip, 1, i ) ).cursor();
			final float[] target = new float[ numberOfCorrelations + 1 ];
			for ( int dz = 0; cursor.hasNext(); ++dz ) {
				target[dz] = cursor.next().getRealFloat();
			}
			target[numberOfCorrelations] = i;
			candidates.add( new PointMatch( new Point( NDIMENSIONS_ZERO ), new Point( target ) ) );
		}
		
		final float[] t = new float[ numberOfCorrelations ];
		final TranslationModelND model = new TranslationModelND( t );
		
		final List<List<PointMatch>> clusters = findClusters(model, candidates, numberOfCorrelations, 
				numberOfZPositions, numberOfZPositions, numberOfCorrelations, numberOfZPositions);
		
		final double[][] result;
		
		if ( clusters.size() > 0 ) {
			result = generateResult( clusters, candidates, numberOfZPositions ); 
		}
		else
			result = new double[ numberOfZPositions ][ 1 ];
		return result;
		
		
	}
	
	
	public static < M extends Model<M> > List< List< PointMatch > > findClusters( final M model, 
			final List< PointMatch > candidates,
			final int iterations,
			final float maxEpsilon,
			final float minInlierRatio,
			final int minNumInliers,
			final float maxTrust ) {
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
		return clusters;
	}
	
	
	public static double[][] generateResult( final List< List< PointMatch > > clusters,
			final List< PointMatch > outliers,
			final int size ) {
		final int nClusters = clusters.size();
		final int nClasses; 
		if ( outliers.size() > 0 )
			nClasses = nClusters + 1;
		else
			nClasses = nClusters;
		final double[][] result = new double[ size ][ nClasses ];
		for ( int i = 0; i < nClusters; ++i ) {
			final List<PointMatch> c = clusters.get( i );
			final int nEntries = c.size();
			for ( int k = 0; k < nEntries; ++k ) {
				final int index = (int)c.get( k ).getP2().getW()[1];
				result[ index ][ i ] = 1.0;
			}
		}
		
		final int nEntries = outliers.size();
		for ( int k = 0; k < nEntries; ++k ) {
			final int index = (int)outliers.get( k ).getP2().getW()[1];
			result[ index ][ nClusters ] = 1.0;
		}
		return null;
	}

}
