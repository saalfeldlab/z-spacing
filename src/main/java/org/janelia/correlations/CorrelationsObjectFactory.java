package org.janelia.correlations;

import java.util.Map.Entry;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObject.Options;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.utility.ConstantRealRandomAccesssible;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class CorrelationsObjectFactory < T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	
	
	
	
	
	/**
	 * @param images
	 */
	public CorrelationsObjectFactory(final RandomAccessibleInterval<T> images) {
		super();
		this.images = images;
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
				
				final Cursor<FloatType> t = Views.flatIterable( correlationsTarget ).cursor();
				final Cursor<FloatType> s = Views.flatIterable( correlationsSource ).cursor();
				
				while ( s.hasNext() ) {
					t.next().set( s.next() );
				}
				
			}
			
			metaMap.put( zRef, meta );
			correlations.put( zRef, localCorrelations );
			
		}
		
		final CorrelationsObject co = new CorrelationsObject( new Options() );

		for ( final Entry<Long, RandomAccessibleInterval<FloatType>> entry : correlations.entrySet() ) {
			co.addCorrelationImage( entry.getKey(), entry.getValue(), metaMap.get( entry.getKey() ) );
		}
		
		return co;
	}

}
