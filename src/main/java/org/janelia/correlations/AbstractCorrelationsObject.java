package org.janelia.correlations;

import java.util.TreeMap;

public abstract class AbstractCorrelationsObject implements
		CorrelationsObjectInterface {
	
	protected TreeMap< Long, Meta > metaMap;
	protected long zMin;
	protected long zMax;
	
	

	/**
	 * @param metaMap
	 */
	public AbstractCorrelationsObject(final TreeMap<Long, Meta> metaMap) {
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
	
	

}
