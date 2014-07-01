package org.janelia.waves.thickness;

import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.janelia.utility.ConstantPair;


public class ZRanges {
	private final HashMap< ConstantPair<Long, Long>, double[]> zCoordinates;
	private long zMin;
	private long zMax;
	
	public ZRanges() {
		super();
		this.zCoordinates     = new HashMap<ConstantPair< Long, Long>,  double[]>();
		
		this.zMin = 0;
		this.zMax = 0;
	}

	public ZRanges(HashMap<ConstantPair<Long, Long>, double[]> zCoordinates) {
		super();
		this.zCoordinates = zCoordinates;
		
		this.zMin = 0;
		this.zMax = 0;
		
		if ( ! this.zCoordinates.isEmpty() ) {
			this.zMax = this.zCoordinates.values().iterator().next().length;
		}
		
		for ( Entry<ConstantPair<Long, Long>, double[]> entry : this.zCoordinates.entrySet() ) {
			if ( entry.getValue().length != ( this.zMax - this.zMin ) ) {
				throw new RuntimeException( "Inconsistency of z Coordinates" );
			}
		}
	}
	
	public ZRanges(HashMap<ConstantPair<Long, Long>, double[]> zCoordinates, final long zMin, final long zMax ) {
		super();
		this.zCoordinates = zCoordinates;
		
		this.zMin = zMin;
		this.zMax = zMax;
		
		for ( Entry<ConstantPair<Long, Long>, double[]> entry : this.zCoordinates.entrySet() ) {
			if ( entry.getValue().length != ( this.zMax - this.zMin ) ) {
				throw new RuntimeException( "Inconsistency of z Coordinates" );
			}
		}
	}
	
	public void addAtXY( final ConstantPair<Long, Long> xy, final double[] coordinateArray ) throws InvalidKeyException {
		if ( this.zCoordinates.containsKey( xy ) ) {
			throw new InvalidKeyException( "Key " + xy + " already exists.");
		}
		
		if ( this.zMax == 0 ) {
			this.zMax = coordinateArray.length;
		} else if ( this.zMax - this.zMin != coordinateArray.length ) {
			throw new RuntimeException( "Inconsistency of z Coordinates" );
		}
		
		
		
		this.zCoordinates.put( xy, coordinateArray );
	}

	/**
	 * @return the zCoordinates
	 */
	public HashMap<ConstantPair<Long, Long>, double[]> getzCoordinates() {
		return zCoordinates;
	}
	
	
	public double[] getzCoordinatesAtXY( final ConstantPair<Long, Long> xy ) {
		return this.zCoordinates.get( xy );
	}
	
	
	public double getzCoordinateAtXYZ( final ConstantPair<Long, Long> xy, final int zGridPosition ) throws InvalidKeyException {
		if ( this.zCoordinates.containsKey( xy ) ) {
			return this.zCoordinates.get( xy )[zGridPosition];
		}
		throw new InvalidKeyException( "Key " + xy + " not in map.");
	}
	
}
