package org.janelia.correlations.storage;

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

import org.janelia.correlations.CrossCorrelationFactory;
import org.janelia.correlations.CrossCorrelationFactoryInterface;
import org.janelia.correlations.storage.CorrelationsObjectInterface.Meta;
import org.janelia.utility.ConstantRealRandomAccesssible;
import org.janelia.utility.sampler.DenseXYSampler;
import org.janelia.utility.sampler.XYSampler;
import org.janelia.utility.tuple.ConstantPair;
import org.janelia.utility.tuple.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class CorrelationsObjectFactory < T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	private final XYSampler sampler;
	private final CrossCorrelationFactoryInterface< T, T, FloatType > ccFactory;
	
	
	/**
	 * @param images
	 * @param sampler
	 * @param ccFactory
	 */
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images,
			final XYSampler sampler,
			final CrossCorrelationFactoryInterface<T, T, FloatType> ccFactory) {
		super();
		this.images = images;
		this.sampler = sampler;
		this.ccFactory = ccFactory;
	}

	/**
	 * @param images
	 */
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images, final XYSampler sampler ) {
		this( images, sampler, new CrossCorrelationFactory< T, T, FloatType >( new FloatType() ) );
	}
	
	
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images) {
		this(images, new DenseXYSampler( images.dimension(0), images.dimension(1) ) );
	}


	public CorrelationsObject create( final long range, final long[] radius ) {
		
		final ConstantPair<TreeMap<Long, RandomAccessibleInterval<FloatType>>, TreeMap<Long, Meta>> correlationsAndMeta = createCorrelationsMetaPair( images, range, radius, sampler, ccFactory );
		final TreeMap<Long, RandomAccessibleInterval<FloatType>> correlations = correlationsAndMeta.getA();
		final TreeMap<Long, Meta> metaMap = correlationsAndMeta.getB();
		
		final CorrelationsObject co = new CorrelationsObject( );

		for ( final Entry<Long, RandomAccessibleInterval<FloatType>> entry : correlations.entrySet() ) {
			co.addCorrelationImage( entry.getKey(), entry.getValue(), metaMap.get( entry.getKey() ) );
		}
		
		return co;
	}
	
	
	public static < U extends RealType< U > > ConstantPair< TreeMap< Long, RandomAccessibleInterval< FloatType> >, TreeMap< Long, Meta > > 
	createCorrelationsMetaPair( final RandomAccessibleInterval< U > images, final long range, final long[] radius, final XYSampler sampler, final CrossCorrelationFactoryInterface< U, U, FloatType > ccFactory ) {
		
		final long stop = images.dimension( 2 ) - 1;
		
		final TreeMap<Long, Meta> metaMap = new TreeMap< Long, Meta > ();
		final TreeMap<Long, RandomAccessibleInterval< FloatType >> correlations = new TreeMap< Long, RandomAccessibleInterval< FloatType > >();
		
		for ( long zRef = 0; zRef <= stop; ++zRef ) {
			
			final long lowerBound = Math.max( 0, zRef - range );
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
					correlationsSource = ccFactory.create( Views.hyperSlice( images, 2, z ), 
							Views.hyperSlice( images, 2, zRef ), 
							radius );
				}
				
				final Iterator<SerializableConstantPair<Long, Long>> it = sampler.iterator();
				final RandomAccess<FloatType> s = correlationsSource.randomAccess();
				final RandomAccess<FloatType> t = correlationsTarget.randomAccess();
				while( it.hasNext() ) {
					final SerializableConstantPair<Long, Long> c = it.next();
					s.setPosition( c.getA(), 0);
					s.setPosition( c.getB(), 1 );
					t.setPosition( s );
					t.get().set( s.get() );
				}
				
			}
			
			
			metaMap.put( zRef, meta );
			correlations.put( zRef, localCorrelations );
			
		}
		
		return ConstantPair.toPair( correlations, metaMap );
	}
	
}
