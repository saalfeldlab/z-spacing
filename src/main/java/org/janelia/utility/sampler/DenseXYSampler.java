package org.janelia.utility.sampler;

import java.util.Iterator;

import org.janelia.utility.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class DenseXYSampler implements XYSampler {

	private final long width;
	private final long height;
	
	

	/**
	 * @param width
	 * @param height
	 */
	public DenseXYSampler(final long width, final long height) {
		super();
		this.width = width;
		this.height = height;
	}

	public class XYIterator implements Iterator<SerializableConstantPair<Long, Long>> {
		
		private long x = -1;
		private long y =  0;
		
		private final long maxX = width - 1;
		private final long maxY = height - 1;

		@Override
		public boolean hasNext() {
			return ( ! ( x == maxX && y == maxY ) );
		}

		@Override
		public SerializableConstantPair<Long, Long> next() {
			if ( x == maxX ) {
				x = 0;
				++y;
			} else
				++x;
			
			return SerializableConstantPair.toPair( x, y );
		}

		@Override
		public void remove() {
			// don't need this
		}
		
	}

	@Override
	public Iterator<SerializableConstantPair<Long, Long>> iterator() {
		return new XYIterator();
	}

}
