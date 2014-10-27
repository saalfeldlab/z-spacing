package org.janelia.correlations;

import java.util.TreeMap;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import org.janelia.correlations.CorrelationsObjectInterface.Meta;

public class CorrelationsObjects {
	
	public static < T extends RealType< T > > SparseCorrelationsObject fromMatrixToSparse( final RandomAccessibleInterval<T> matrix, final long x, final long y, final int comparisonRange ) {
		assert matrix.numDimensions() == 2;
		assert matrix.dimension( 0 ) == matrix.dimension( 1 );
		assert comparisonRange <= matrix.dimension( 0 );
		final TreeMap<Long, Meta> metaMap           = new TreeMap< Long, Meta >();
		final RandomAccess<T> ra = matrix.randomAccess();
		
		final SparseCorrelationsObject sco = new SparseCorrelationsObject();
		
		for ( int z = 0; z < matrix.dimension( 0 ); ++z ) {
			ra.setPosition( z, 0 );
			final int lower = Math.max( z - comparisonRange, 0 );
			final int upper = Math.min( z + comparisonRange, (int)matrix.dimension( 0 ) );
			
			final Meta meta = new Meta();
			meta.zCoordinateMin = lower;
			meta.zCoordinateMax = upper;
			meta.zPosition      = z;
			metaMap.put( (long) z, meta );
			final double[] c = new double[ upper - lower ];
			for ( int l = lower; l < upper; ++l ) {
				ra.setPosition( l, 1 );
				c[ l - lower ] = ra.get().getRealDouble();
			}
			sco.addCorrelationsAt( x, y, z, c, meta );
		}
		
		return sco;
	}

}
