package org.janelia.correlations.storage;

import java.util.Set;
import java.util.TreeMap;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.tuple.SerializableConstantPair;

public abstract class AbstractCorrelationsObject implements
		CorrelationsObjectInterface {
	
	protected TreeMap< Long, Meta > metaMap;
	protected long zMin;
	protected long zMax;
	
	public float tolerance = 1e-5f;
	
	
	public AbstractCorrelationsObject() {
		this( new TreeMap< Long,Meta >() );
	}
	
	
	
	/**
	 * @param metaMap
	 */
	public AbstractCorrelationsObject( final TreeMap<Long, Meta> metaMap ) {
		super();
		this.metaMap = metaMap;
		if ( this.metaMap.size() > 0 ) {
			this.zMin = metaMap.firstEntry().getValue().zCoordinateMin;
			this.zMax = metaMap.firstEntry().getValue().zCoordinateMax;
		} else {
			this.zMin = 0;
			this.zMax = 0;
		}
		for ( final Meta value : this.metaMap.values() ) {
			updateMinMax( value );
		}
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
	
	protected void addToMeta( final long pos, final Meta meta ) {
		this.metaMap.put( pos, meta );
		this.updateMinMax( meta );
	}
	
	protected void updateMinMax( final Meta meta ) {
		this.zMin = Math.min( this.zMin, meta.zPosition );
		this.zMax = Math.max( this.zMax, meta.zPosition + 1 );
	}
	
	@Override
	public boolean equalsMeta( final CorrelationsObjectInterface other ) {
		return this.metaMap.equals( other.getMetaMap() );
	}
	
	@Override
	public boolean equalsXYCoordinates( final CorrelationsObjectInterface other ) {
		return this.getXYCoordinates().equals( other.getXYCoordinates() );
	}
	
	@Override
	public boolean equals( final Object other ) {
		if ( other instanceof AbstractCorrelationsObject ) {
			final AbstractCorrelationsObject aco = (AbstractCorrelationsObject) other;
			
			if ( this.equalsMeta( aco ) ) {
				
				if ( this.equalsXYCoordinates( aco ) ) {
					final Set<SerializableConstantPair<Long, Long>> XYCoordinates = this.getXYCoordinates();
					for ( final SerializableConstantPair<Long, Long> xy : XYCoordinates ) {
						
						final ArrayImg<DoubleType, DoubleArray> m1 = this.toMatrix( xy.getA(), xy.getB() );
						final ArrayImg<DoubleType, DoubleArray> m2 = aco.toMatrix( xy.getA(),  xy.getB() );
						
						if ( m1.numDimensions() != m2.numDimensions() )
							return false;
						
						if ( m1.dimension( 0 ) != m2.dimension( 0 ) || m1.dimension( 1 ) != m2.dimension( 1 ) )
							return false;
						
						final ArrayCursor<DoubleType> m1c = m1.cursor();
						final ArrayCursor<DoubleType> m2c = m2.cursor();
						
						while( m1c.hasNext() ) {
							final double val1 = m1c.next().get();
							final double val2 = m2c.next().get();
							if ( Double.isNaN( val1 ) && Double.isNaN( val2 ) )
								continue;
							if ( ! ( Math.abs( val1 - val2 ) < tolerance ) )
								return false;
						}
					}
				}
				else
					return false;
				
			}
			else
				return false;
			
			
			
			return true;
		}
		else 
			return false;
	}

	
	
}
