package org.janelia.correlations;

import java.util.HashMap;
import java.util.TreeMap;

import org.janelia.utility.ConstantPair;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public interface CorrelationsObjectInterface {
	
	public static class Meta {
		public long zPosition;
		public long zCoordinateMin;
		public long zCoordinateMax;
		
		@Override
		public String toString() {
			return new String("zPosition=" + this.zPosition +
					",zCoordinateMin=" + this.zCoordinateMin +
					",zCoordinateMax=" + this.zCoordinateMax);
		}
	}

	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> > extractCorrelationsAt(long x, long y, long z);
	
	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > extractDoubleCorrelationsAt(long x, long y, long z);
	
	public long getzMin();
	
	public long getzMax();
	
	public TreeMap<Long, Meta> getMetaMap();
	
}
