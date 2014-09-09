package org.janelia.utility.sampler;

import java.util.Iterator;

import org.janelia.utility.ConstantPair;

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

	public class XYIterator implements Iterator<ConstantPair<Long, Long>> {
		
		private long x = -1;
		private long y =  0;
		
		private final long maxX = width - 1;
		private final long maxY = height - 1;

		@Override
		public boolean hasNext() {
			return ( ! ( x == maxX && y == maxY ) );
		}

		@Override
		public ConstantPair<Long, Long> next() {
			if ( x == maxX ) {
				x = 0;
				++y;
			} else
				++x;
			
			return ConstantPair.toPair( x, y );
		}

		@Override
		public void remove() {
			// don't need this
		}
		
	}

	@Override
	public Iterator<ConstantPair<Long, Long>> iterator() {
		return new XYIterator();
	}

}
