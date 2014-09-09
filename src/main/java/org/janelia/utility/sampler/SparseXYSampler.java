/**
 * 
 */
package org.janelia.utility.sampler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.janelia.utility.ConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SparseXYSampler implements XYSampler {

	private final List< ConstantPair< Long, Long > > coords;
	/**
	 * @param coords
	 */
	public SparseXYSampler( final List<ConstantPair<Long, Long>> coords ) {
		super();
		this.coords = coords;
	}
	
	
	public SparseXYSampler() {
		this( new ArrayList<ConstantPair<Long, Long>>() );
	}


	@Override
	public Iterator<ConstantPair<Long, Long>> iterator() {
		return coords.iterator();
	}

}
