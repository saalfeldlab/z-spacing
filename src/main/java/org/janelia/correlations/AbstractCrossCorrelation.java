package org.janelia.correlations;

import net.imglib2.Positionable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 * @param <U>
 */
public abstract class AbstractCrossCorrelation  < T extends RealType< T >, U extends RealType< U >, S extends RealType< S > & NativeType< S > > implements RandomAccessibleInterval< S > {
	
	protected final RandomAccessibleInterval<T> img1;
	protected final RandomAccessibleInterval<U> img2;
	protected final ArrayImg< S, ? > correlations;
	protected final long[] dim;
	protected final long[] r;
	protected final long[] min;
	protected final long[] max;
	
	
	/**
	 * @param img1
	 * @param img2
	 */
	public AbstractCrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final S type ) {
		super();
		assert img1.numDimensions() == img2.numDimensions(): "Mismatch in number of dimensions";
		
		for ( int d = 0; d < img1.numDimensions(); ++ d ) {
			assert img1.dimension( d ) == img2.dimension( d ): String.format( "Mismatch in dimension %d", d );
		}
		
		assert r.length == img1.numDimensions() || r.length == 1: "Mismatch in number of dimensions and radii";
		
		this.img1 = img1;
		this.img2 = img2;
		
		this.dim = new long[ img1.numDimensions() ];
		img1.dimensions( dim ); // write dimensions into dim
		this.min = new long[ dim.length ];
		this.max = new long[ dim.length ];
		for ( int d = 0; d < dim.length; ++d ) {
			this.min[d] = 0;
			this.max[d] = dim[d] - 1;
		}
		
		this.correlations = new ArrayImgFactory< S >().create( dim, type );
		
		
		
		this.r = new long[ this.dim.length ];
		setRadius ( r );
	}
	
	
	public void setRadius( final long[] r ) {
		assert r.length == 1 || r.length == this.r.length: "Dimension mismatch!";
		if ( r.length == 1 )
			setRadius( r[0] );
		else
			System.arraycopy( r, 0, this.r, 0, r.length );
	}
	
	
	public void setRadius( final long r ) {
		for ( int i = 0; i < this.r.length; ++i )
			this.r[ i ] = r;
	}
	
	
	@Override
	public int numDimensions() {
		return this.dim.length;
	}

	@Override
	public long min(final int d) {
		return this.min[ d ];
	}

	@Override
	public void min(final long[] min) {
		for (int i = 0; i < min.length; i++) {
			min[i] = this.min[i];
		}
	}

	@Override
	public void min(final Positionable min) {
		min.setPosition( this.min );
	}

	@Override
	public long max(final int d) {
		// TODO Auto-generated method stub
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
		max.setPosition(this.max);
	}

	@Override
	public double realMin(final int d) {
		return min(d);
	}

	@Override
	public void realMin(final double[] min) {
		for (int i = 0; i < min.length; i++) {
			min[i] = this.min[i];
		}
	}

	@Override
	public void realMin(final RealPositionable min) {
		min( min );
	}

	@Override
	public double realMax(final int d) {
		return max( d );
	}

	@Override
	public void realMax(final double[] max) {
		for (int i = 0; i < max.length; i++) {
			max[i] = this.max[i];
		}
	}

	@Override
	public void realMax(final RealPositionable max) {
		max( max );
	}

	@Override
	public void dimensions(final long[] dimensions) {
		for (int i = 0; i < dimensions.length; i++) {
			dimensions[i] = this.dim[i];
		}
	}

	@Override
	public long dimension(final int d) {
		return this.dim[d];
	}

}
