package org.janelia.correlations;


import java.util.TreeMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 * Implements the {@link CorrelationsObjectInterface} for test purposes.
 *
 */
public class DummyCorrelationsObject implements CorrelationsObjectInterface {
		
	
	private final long zMin;
	private final long zMax;
	private final int range;
	private final int nData;
	private final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs;
	private final TreeMap< Long, Meta > metaMap;
	
	
		
	public DummyCorrelationsObject(
			long zMin,
			long zMax,
			int range,
			int nData,
			TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>> corrs,
			TreeMap< Long, Meta > metaMap ) {
		super();
		this.zMin    = zMin;
		this.zMax    = zMax;
		this.range   = range;
		this.nData   = nData;
		this.corrs   = corrs;
		this.metaMap = metaMap;
	}

	public long getzMin() {
		return (long) zMin;
	}
	
	public long getzMax() {
		return (long) zMax;
	}
	
	public TreeMap<Long, Meta> getMetaMap() {
		return this.metaMap;
	}

	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> extractCorrelationsAt(
			long x, long y, long z) {
		return null;
	}


	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
			long x, long y, long z) {
		return corrs.get( new ConstantTriple<Long, Long, Long>( x, y, z) );
	}
}