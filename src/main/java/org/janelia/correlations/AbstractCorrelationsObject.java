package org.janelia.correlations;

import java.util.Set;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.utility.ConstantPair;

public abstract class AbstractCorrelationsObject implements
		CorrelationsObjectInterface {
	
	protected TreeMap< Long, Meta > metaMap;
	protected long zMin;
	protected long zMax;
	
	public float tolerance = 1e-5f;
	
	

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
					final Set<ConstantPair<Long, Long>> XYCoordinates = this.getXYCoordinates();
					for ( final ConstantPair<Long, Long> xy : XYCoordinates ) {
						for ( final Long z : metaMap.keySet() ) {
							final RandomAccessibleInterval<DoubleType> c1 = this.extractDoubleCorrelationsAt( xy.getA(), xy.getB(), z ).getA();
							final RandomAccessibleInterval<DoubleType> c2 = aco.extractDoubleCorrelationsAt( xy.getA(), xy.getB(), z ).getA();
							
							if ( c1.dimension( 0 ) != c2.dimension( 0 ) )
								return false;
							
							final Cursor<DoubleType> cc1 = Views.flatIterable( c1 ).cursor();
							final Cursor<DoubleType> cc2 = Views.flatIterable( c2 ).cursor();
							
							while ( cc1.hasNext() ) {
								cc1.fwd();
								cc2.fwd();
								if ( ! ( Math.abs( cc1.get().get() - cc2.get().get() ) < tolerance ) )
									return false;
							}
							
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
