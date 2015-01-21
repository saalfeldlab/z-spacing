/**
 * 
 */
package org.janelia.correlations.storage;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.FloatingPointIntegralCrossCorrelation;
import org.janelia.utility.tuple.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ListCorrelationsObject< T extends RealType< T > > extends
		AbstractCorrelationsObject {
	
	private final TreeMap< Long, List< FloatingPointIntegralCrossCorrelation< T, T, FloatType > > > correlationsMap;

	/**
	 * @param metaMap
	 * @param correlationsMap
	 */
	public ListCorrelationsObject(
			final TreeMap<Long, Meta> metaMap,
			final TreeMap<Long, List< FloatingPointIntegralCrossCorrelation<T, T, FloatType> > > correlationsMap) {
		super(metaMap);
		this.correlationsMap = correlationsMap;
	}

	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y) {
		final ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( metaMap.size(), metaMap.size() );
		this.toMatrix( x,  y, matrix );
		return matrix;
	}

	@Override
	public void toMatrix(final long x, final long y,
			final RandomAccessibleInterval<DoubleType> matrix) {
		for ( final DoubleType m : Views.flatIterable( matrix ) ) {
			m.set( Double.NaN );
		}
		final long[] xy = new long[] { x, y };
		for ( final Entry<Long, Meta> entry : metaMap.entrySet() ) {
			final Long zRef = entry.getKey();
			final Meta meta = entry.getValue();
			final List<FloatingPointIntegralCrossCorrelation<T, T, FloatType>> al = correlationsMap.get( zRef );
			final RandomAccess<DoubleType> ra = Views.hyperSlice( matrix, 0, zRef ).randomAccess();
			ra.setPosition( meta.zCoordinateMin, 0 );
			final long range = meta.zCoordinateMax - meta.zCoordinateMin;
			for ( int z = 0; z < range; ++z ) {
				final RandomAccess<FloatType> ccRa = al.get( z ).randomAccess();
				ccRa.setPosition( xy );
//				ra.get().set( Math.max( 0.0, ccRa.get().get() ) );
				ra.get().set( ccRa.get().get() );
				ra.fwd( 0 );
			}
		}
	}

	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y,
			final long zMin, final long zMax) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getxMin() {
		return 0;
	}

	@Override
	public long getyMin() {
		return 0;
	}

	@Override
	public long getxMax() {
		return correlationsMap.firstEntry().getValue().get( 0 ).dimension( 0 );
	}

	@Override
	public long getyMax() {
		return correlationsMap.firstEntry().getValue().get( 0 ).dimension( 1 );
	}

	@Override
	public Set<SerializableConstantPair<Long, Long>> getXYCoordinates() {
		final FloatingPointIntegralCrossCorrelation<T, T, FloatType> img = correlationsMap.firstEntry().getValue().get( 0 );
		final Set< SerializableConstantPair< Long, Long > > coordinates = new TreeSet<SerializableConstantPair<Long,Long>>();
		for ( final Cursor<FloatType> c = Views.flatIterable( img ).cursor(); c.hasNext(); ) {
			c.fwd();
			coordinates.add( SerializableConstantPair.toPair( c.getLongPosition( 0 ), c.getLongPosition( 1 ) ) );
		}
		return coordinates;
	}

	@Override
	public <U extends RealType<U> & NativeType<U>> void toCorrelationStripe(
			final long x, final long y, final RandomAccessibleInterval<U> stripe) {
		// TODO Auto-generated method stub

	}

}
