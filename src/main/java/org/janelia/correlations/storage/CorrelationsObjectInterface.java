package org.janelia.correlations.storage;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.utility.tuple.ConstantPair;
import org.janelia.utility.tuple.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 * The {@link CorrelationsObjectInterface} provides functions to get correlations for any point in an image volume given it's x,y,z coordinates.
 * The function {@link CorrelationsObjectInterface#getMetaMap()} returns a map that stores {@link Meta} information for each z-slice. 
 *
 */
// get Serializable out of interface, only needed in SparseCorrelations
public interface CorrelationsObjectInterface extends Serializable {
	
	public static class Meta implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2529898506717425892L;
		public long zPosition;
		public long zCoordinateMin;
		public long zCoordinateMax;
		
		@Override
		public String toString() {
			return new String("zPosition=" + this.zPosition +
					",zCoordinateMin=" + this.zCoordinateMin +
					",zCoordinateMax=" + this.zCoordinateMax);
		}
		
		
		@Override
		public boolean equals( final Object other ) {
			if ( other instanceof Meta ) {
				final Meta meta = (Meta) other;
				return (
						this.zPosition      == meta.zPosition &&
						this.zCoordinateMax == meta.zCoordinateMax &&
						this.zCoordinateMin == meta.zCoordinateMin
						);
			}
			else
				return false;
		}
	}

	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> > extractCorrelationsAt(long x, long y, long z);
	
	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > extractDoubleCorrelationsAt(long x, long y, long z);
	
	public ArrayImg<DoubleType, DoubleArray> toMatrix( long x, long y );
	
	void toMatrix( final long x, final long y, RandomAccessibleInterval< DoubleType > matrix );
	
	public ArrayImg<DoubleType, DoubleArray> toMatrix( long x, long y, long zMin,
			long zMax );
	
	public long getzMin();
	
	public long getxMin();
	
	public long getyMin();
	
	public long getzMax();
	
	public long getxMax();
	
	public long getyMax();
	
	public TreeMap<Long, Meta> getMetaMap();
	
	public boolean equalsMeta( CorrelationsObjectInterface other );
	
	public boolean equalsXYCoordinates( final CorrelationsObjectInterface other );
	
	public Set< SerializableConstantPair< Long, Long > > getXYCoordinates();
	
	public < T extends RealType< T> & NativeType< T > > void toCorrelationStripe( long x, long y, RandomAccessibleInterval< T > stripe );

	@Override
	public boolean equals( Object other );
	
}
