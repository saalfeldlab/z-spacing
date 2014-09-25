package org.janelia.correlations;


import java.util.Set;
import java.util.TreeMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.utility.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 * Implements the {@link CorrelationsObjectInterface} for test purposes.
 *
 */
public class DummyCorrelationsObject implements CorrelationsObjectInterface {
		
	
	private final long zMin;
	private final long zMax;
	private final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs;
	private final TreeMap< Long, Meta > metaMap;
		
	public DummyCorrelationsObject(
			final long zMin,
			final long zMax,
			final int range,
			final int nData,
			final TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>> corrs,
			final TreeMap< Long, Meta > metaMap ) {
		super();
		this.zMin    = zMin;
		this.zMax    = zMax;
		this.corrs   = corrs;
		this.metaMap = metaMap;
	}

	@Override
	public long getzMin() {
		return zMin;
	}
	
	@Override
	public long getzMax() {
		return zMax;
	}
	
	@Override
	public TreeMap<Long, Meta> getMetaMap() {
		return this.metaMap;
	}

	@Override
	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> extractCorrelationsAt(
			final long x, final long y, final long z) {
		return null;
	}


	@Override
	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
			final long x, final long y, final long z) {
		return corrs.get( new ConstantTriple<Long, Long, Long>( x, y, z) );
	}

	/**
	 * Is implementation of this method necessary for dummy?
	 */
	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Is implementation of this method necessary for dummy?
	 */
	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y,
			final long zMin, final long zMax) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Is implementation of this method necessary for dummy?
	 */
	@Override
	public void toMatrix(final long x, final long y,
			final RandomAccessibleInterval<DoubleType> matrix) {

	}

	@Override
	public boolean equalsMeta(final CorrelationsObjectInterface other) {
		// TODO this dummy should always return false
		return false;
	}

	@Override
	public Set<SerializableConstantPair<Long, Long>> getXYCoordinates() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equalsXYCoordinates(final CorrelationsObjectInterface other) {
		// TODO Auto-generated method stub
		return false;
	}

}