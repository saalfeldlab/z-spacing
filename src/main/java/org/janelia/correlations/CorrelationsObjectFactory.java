package org.janelia.correlations;

import java.util.ArrayList;
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

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class CorrelationsObjectFactory < T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	private final XYSampler sampler;
	
	public static interface XYSampler extends Iterable< ConstantPair< Long, Long > > {
		
	}
	
	public static class DenseSampler implements XYSampler {
		
		private final long width;
		private final long height;
		
		

		/**
		 * @param width
		 * @param height
		 */
		public DenseSampler(final long width, final long height) {
			super();
			this.width = width;
			this.height = height;
		}

		public class XYIterator implements Iterator<ConstantPair<Long, Long>> {
			
			private long x = -1;
			private long y =  0;
			
			private final long maxX = width - 1;
			private final long maxY = height - 1;

			@Override
			public boolean hasNext() {
				return ( ! ( x == maxX && y == maxY ) );
			}

			@Override
			public ConstantPair<Long, Long> next() {
				if ( x == maxX ) {
					x = 0;
					++y;
				} else
					++x;
				
				return new ConstantPair< Long, Long >( x, y );
			}

			@Override
			public void remove() {
				// don't need this
			}
			
		}

		@Override
		public Iterator<ConstantPair<Long, Long>> iterator() {
			return new XYIterator();
		}
		
	}
	
	
	public static class SparseSampler implements XYSampler {
		
		private final ArrayList< ConstantPair< Long, Long > > coords;
		/**
		 * @param coords
		 */
		public SparseSampler(final ArrayList<ConstantPair<Long, Long>> coords) {
			super();
			this.coords = coords;
		}
		
		
		public SparseSampler() {
			super();
			this.coords = new ArrayList<ConstantPair<Long,Long>>();
		}


		@Override
		public Iterator<ConstantPair<Long, Long>> iterator() {
			return coords.iterator();
		}
		
	}
	
	
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
		this.sampler = new DenseSampler( images.dimension(0), images.dimension(1) );
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
							Views.hyperSlice( images, 2, z ), 
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
