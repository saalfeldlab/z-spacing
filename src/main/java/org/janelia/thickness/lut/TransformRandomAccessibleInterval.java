/**
 * 
 */
package org.janelia.thickness.lut;

import java.util.ArrayList;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class TransformRandomAccessibleInterval implements
		RandomAccessibleInterval<AbstractLUTRealTransform> {
	
	private final int numDimensions;
	private final long[] max;
	private final long[] min;
	private final double[] minDouble;
	private final ArrayList<AbstractLUTRealTransform> transforms;
	
	public TransformRandomAccessibleInterval(final long[] max,
			final ArrayList<AbstractLUTRealTransform> transforms) {
		super();
		this.numDimensions = max.length;
		this.max = max;
		this.min = new long[ max.length ];
		this.minDouble = new double[ max.length ];
		this.transforms = transforms;
	}

	public class TransformRandomAccess extends RealPoint implements RandomAccess<AbstractLUTRealTransform> {
		
	    private final int[] multipliers;
		
		TransformRandomAccess( final double[] position ) {
			super(position);
			this.multipliers = new int[ max.length ];
			this.multipliers[0] = 1;
			for ( int d = 0; d < max.length - 1; ++ d ) {
				this.multipliers[ d + 1 ] = (int) (multipliers[d] * max[ d ]);
			}
		}
		
		TransformRandomAccess( final double[] position, final int[] multipliers ) {
			super( position );
			this.multipliers = multipliers;
		}
		

		@Override
		public void localize(final long[] position) {
			for (int d = 0; d < position.length; d++) {
				position[d] = (long) this.position[d];
			}
			
		}

		@Override
		public int getIntPosition(final int d) {
			return (int)getDoublePosition(d);
		}

		@Override
		public long getLongPosition(final int d) {
			return (long) getDoublePosition(d);
		}

		@Override
		public AbstractLUTRealTransform get() {
			int index = 0;
			for ( int d = 0; d < numDimensions(); ++d ) {
				index += this.position[d] * this.multipliers[d];
			}
			return transforms.get( index );
		}

		@Override
		public TransformRandomAccess copy() {
			return new TransformRandomAccess( position, multipliers );
		}

		@Override
		public TransformRandomAccess copyRandomAccess() {
			return copy();
		}

		@Override
		public void localize(final int[] position) {
			for (int i = 0; i < position.length; i++) {
				position[i] = (int) this.position[i];
			}
		}
		
	}

	@Override
	public RandomAccess<AbstractLUTRealTransform> randomAccess() {
		return new TransformRandomAccess( this.minDouble );
	}

	@Override
	public RandomAccess<AbstractLUTRealTransform> randomAccess(final Interval interval) {
		return this.randomAccess();
	}

	@Override
	public int numDimensions() {
		return this.numDimensions;
	}

	@Override
	public long min(final int d) {
		return 0;
	}

	@Override
	public void min(final long[] min) {
		for (int i = 0; i < min.length; i++) {
			min[i] = 0;
		}
	}

	@Override
	public void min(final Positionable min) {
		for ( int i = 0; i < min.numDimensions(); ++i ) {
			min.setPosition( 0, i );
		}
	}

	@Override
	public long max(final int d) {
		return this.max[d];
	}

	@Override
	public void max(final long[] max) {
		for (int i = 0; i < max.length; i++) {
			max[i] = this.max[i];
		}
	}

	@Override
	public void max(final Positionable max) {
		max.setPosition( this.max );
	}

	@Override
	public double realMin(final int d) {
		return 0.0;
	}

	@Override
	public void realMin(final double[] min) {
		for (int i = 0; i < min.length; i++) {
			min[i] = 0.0;
		}
	}

	@Override
	public void realMin(final RealPositionable min) {
		for ( int i = 0; i < min.numDimensions(); ++i ) {
			min.setPosition( 0.0, i );
		}
	}

	@Override
	public double realMax(final int d) {
		return this.max[ d ];
	}

	@Override
	public void realMax(final double[] max) {
		for (int d = 0; d < max.length; d++) {
			max[d] = this.max[d];
		}
	}

	@Override
	public void realMax(final RealPositionable max) {
		max.setPosition( this.max );
	}

	@Override
	public void dimensions(final long[] dimensions) {
		for (int d = 0; d < dimensions.length; d++) {
			dimensions[d] = this.max[d];
		}
	}

	@Override
	public long dimension(final int d) {
		return this.max[d];
	}
	

}
