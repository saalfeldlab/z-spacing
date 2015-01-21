package org.janelia.correlations.storage;


import java.util.Set;
import java.util.TreeMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.tuple.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 * Implements the {@link CorrelationsObjectInterface} for test purposes.
 *
 */
public class DummyCorrelationsObject implements CorrelationsObjectInterface {
		
	
	private static final long serialVersionUID = 215592469910432642L;
	private final long zMin;
	private final long zMax;
	private final TreeMap< Long, Meta > metaMap;
		
	public DummyCorrelationsObject(
			final long zMin,
			final long zMax,
			final int range,
			final int nData,
			final TreeMap< Long, Meta > metaMap ) {
		super();
		this.zMin    = zMin;
		this.zMax    = zMax;
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
		return 1;
	}

	@Override
	public long getyMax() {
		return 1;
	}

}