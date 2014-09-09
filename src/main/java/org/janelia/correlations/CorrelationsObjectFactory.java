package org.janelia.correlations;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantRealRandomAccesssible;
import org.janelia.utility.sampler.DenseXYSampler;
import org.janelia.utility.sampler.XYSampler;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class CorrelationsObjectFactory < T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	private final XYSampler sampler;
	
	
	/**
	 * @param images
	 */
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images, final XYSampler sampler ) {
		super();
		this.images = images;
		this.sampler = sampler;
	}
	
	
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images) {
		super();
		this.images = images;
		this.sampler = new DenseXYSampler( images.dimension(0), images.dimension(1) );
	}


	public CorrelationsObjectInterface create( final long range, final long[] radius ) {
		
		final long stop = images.dimension( 2 ) - 1;
		
		final TreeMap<Long, Meta> metaMap = new TreeMap< Long, Meta > ();
		final TreeMap<Long, RandomAccessibleInterval< FloatType >> correlations = new TreeMap< Long, RandomAccessibleInterval< FloatType > >();
		
		for ( long zRef = 0; zRef <= stop; ++zRef ) {
			
			final long lowerBound = Math.max( 0, zRef - range);
			final long upperBound = Math.min( stop, zRef + range );
			
			final Meta meta = new Meta();
			meta.zCoordinateMin = lowerBound;
			meta.zCoordinateMax = upperBound + 1;
			meta.zPosition      = zRef;
			
			final ArrayImg<FloatType, FloatArray> localCorrelations = ArrayImgs.floats(images.dimension( 0 ), images.dimension( 1 ), ( upperBound - lowerBound) + 1 );
			
			
			for ( long z = lowerBound; z <= upperBound; ++z ) {
				final long relativePosition = z - lowerBound;
				final IntervalView<FloatType> correlationsTarget = Views.hyperSlice( localCorrelations, 2, relativePosition );
				
				final RandomAccessibleInterval< FloatType > correlationsSource;
				
				if ( z < zRef ) {
					final Meta previousMeta  = metaMap.get( z );
					// position of zRef within correlations image at z is zRef - previousMeta.zCoordinateMin
					correlationsSource = Views.hyperSlice( correlations.get( z ), 2, zRef - previousMeta.zCoordinateMin );
				} else if ( z == zRef ) {
					correlationsSource = Views.interval( Views.raster( new ConstantRealRandomAccesssible< FloatType >( 2, new FloatType( 1.0f ) ) ), correlationsTarget );
				} else {
					correlationsSource = new CrossCorrelation<T, T>( 
							Views.hyperSlice( images, 2, z ), 
							Views.hyperSlice( images, 2, zRef ), 
							radius );
				}
				
				final Iterator<ConstantPair<Long, Long>> it = sampler.iterator();
				final RandomAccess<FloatType> s = correlationsSource.randomAccess();
				final RandomAccess<FloatType> t = correlationsTarget.randomAccess();
				while( it.hasNext() ) {
					final ConstantPair<Long, Long> c = it.next();
					s.setPosition( c.getA(), 0);
					s.setPosition( c.getB(), 1 );
					t.setPosition( s );
					t.get().set( s.get() );
				}
				
			}
			
			metaMap.put( zRef, meta );
			correlations.put( zRef, localCorrelations );
			
		}
		
		final CorrelationsObject co = new CorrelationsObject( );

		for ( final Entry<Long, RandomAccessibleInterval<FloatType>> entry : correlations.entrySet() ) {
			co.addCorrelationImage( entry.getKey(), entry.getValue(), metaMap.get( entry.getKey() ) );
		}
		
		return co;
	}
	
}
