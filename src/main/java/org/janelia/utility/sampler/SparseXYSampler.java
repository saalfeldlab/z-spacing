/**
 * 
 */
package org.janelia.utility.sampler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.janelia.utility.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SparseXYSampler implements XYSampler {

	private final List< SerializableConstantPair< Long, Long > > coords;
	/**
	 * @param coords
	 */
	public SparseXYSampler( final List<SerializableConstantPair<Long, Long>> coords ) {
		super();
		this.coords = coords;
	}
	
	
	public SparseXYSampler() {
		this( new ArrayList<SerializableConstantPair<Long, Long>>() );
	}


	@Override
	public Iterator<SerializableConstantPair<Long, Long>> iterator() {
		return coords.iterator();
	}

}
