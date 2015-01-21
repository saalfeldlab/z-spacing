package org.janelia.correlations.storage;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.CrossCorrelation;
import org.janelia.correlations.CrossCorrelation.CrossCorrelationRandomAccess;
import org.janelia.correlations.CrossCorrelation.TYPE;
import org.janelia.correlations.storage.CorrelationsObjectInterface.Meta;
import org.janelia.utility.sampler.DenseXYSampler;
import org.janelia.utility.sampler.XYSampler;
import org.janelia.utility.tuple.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class SparseCorrelationsObjectFactory < T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	private final XYSampler sampler;
	private final CrossCorrelation.TYPE type;
	
	
	/**
	 * @param images
	 */
	public SparseCorrelationsObjectFactory(final RandomAccessibleInterval<T> images, final XYSampler sampler, final CrossCorrelation.TYPE type ) {
		super();
		this.images = images;
		this.sampler = sampler;
		this.type = type;
	}
	
	public SparseCorrelationsObjectFactory(final RandomAccessibleInterval<T> images, final XYSampler sampler ) {
		this( images, sampler, CrossCorrelation.TYPE.STANDARD );
	}
	
	public SparseCorrelationsObjectFactory(final RandomAccessibleInterval<T> images, final CrossCorrelation.TYPE type ) {
		this(images, new DenseXYSampler( images.dimension(0), images.dimension(1) ), type );
	}
	
	
	public SparseCorrelationsObjectFactory(final RandomAccessibleInterval<T> images) {
		this( images, new DenseXYSampler( images.dimension(0), images.dimension(1) ) );
	}


	public CorrelationsObjectInterface create( final long range, final long[] radius ) {
		
		final long stop = images.dimension( 2 ) - 1;
		
		final TreeMap<Long, Meta> metaMap = new TreeMap< Long, Meta > ();
		final TreeMap<SerializableConstantPair<Long, Long>, TreeMap<Long, double[]>> correlations = new TreeMap< SerializableConstantPair< Long, Long >, TreeMap< Long, double[] > >();
		
		final Iterator<SerializableConstantPair<Long, Long>> sampleIterator = sampler.iterator();
		int count = 0;
		while ( sampleIterator.hasNext() ) {
			final SerializableConstantPair<Long, Long> xy = sampleIterator.next();
			// as we just created correlations, nothing present at XY yet; it is the user's responsibility to make sure, there's no duplicate coordinates in sampler
			final TreeMap<Long, double[]> correlationsAtXY = new TreeMap<Long, double[]>();
			correlations.put( xy, correlationsAtXY );
			final Long x = xy.getA();
			final Long y = xy.getB();
			
			for ( long zRef = 0; zRef <= stop; ++zRef ) {
				
				final long lowerBound = Math.max( 0, zRef - range);
				final long upperBound = Math.min( stop, zRef + range );
				
				final Meta meta = new Meta();
				meta.zCoordinateMin = lowerBound;
				meta.zCoordinateMax = upperBound + 1;
				meta.zPosition      = zRef;
				
				if ( count == 0 )
					metaMap.put( zRef, meta );
				
				// as we just created correlationsAtXY, nothing present at zRef yet
				final double[] correlationsAt = new double[ (int) (meta.zCoordinateMax - meta.zCoordinateMin) ];
				correlationsAtXY.put( zRef, correlationsAt);
				
				for ( long z = lowerBound; z <= upperBound; ++z ) {
					final int relativePosition = (int) (z - lowerBound);
					
					if ( z < zRef ) {
						final Meta previousMeta = metaMap.get( z );
						correlationsAt[ relativePosition ] = correlationsAtXY.get( z )[ (int) (zRef - previousMeta.zCoordinateMin) ];
					} else if ( z == zRef ) {
						correlationsAt[ relativePosition ] = 1.0;
					} else {
						final CrossCorrelation<T, T, FloatType > cc = new CrossCorrelation< T, T, FloatType >(
								Views.hyperSlice( images, 2, z ),
								Views.hyperSlice( images, 2, zRef ),
								radius,
								this.type,
								new FloatType() );
						final CrossCorrelationRandomAccess ra = cc.randomAccess();
						ra.setPosition( new long[] { xy.getA(), xy.getB() } );
						correlationsAt[ relativePosition ] = ra.get().getRealDouble();
					}
				}
				
			}
			++count;
		}
		
		final SparseCorrelationsObject sco = new SparseCorrelationsObject();

		for ( final Entry<SerializableConstantPair<Long, Long>, TreeMap<Long, double[]>> entry : correlations.entrySet() ) 
		{
			final Long x = entry.getKey().getA();
			final Long y = entry.getKey().getB();
			for ( final Entry<Long, double[]> corrEntry : entry.getValue().entrySet() ) {
				final Long z = corrEntry.getKey();
				if ( x == 0 && y == 0 )
					sco.addCorrelationsAt( x, y, z, corrEntry.getValue(), metaMap.get( z ) );
				else
					sco.addCorrelationsAt( x, y, z, corrEntry.getValue() );
			}
		}
		
		return sco;
	}
	
	
	public static < T extends RealType< T > > SparseCorrelationsObject fromMatrix( final RandomAccessibleInterval<T> matrix, final long x, final long y, final int comparisonRange ) {
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
