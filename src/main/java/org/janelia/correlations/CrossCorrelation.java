package org.janelia.correlations;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.BitArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 * @param <U>
 */
public class CrossCorrelation < T extends RealType< T >, U extends RealType< U > > implements RandomAccessibleInterval< FloatType > {
	
	private final RandomAccessibleInterval<T> img1;
	private final RandomAccessibleInterval<U> img2;
	private final ArrayImg< FloatType, FloatArray > correlations;
	private final ArrayImg< BitType, BitArray > calculatedCheck;
	private final long[] dim;
	private final long[] r;
	private final long[] min;
	private final long[] max;
	
	

	public CrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r ) {
		super();
		assert img1.numDimensions() == img2.numDimensions(): "Mismatch in number of dimensions";
		
		for ( int d = 0; d < img1.numDimensions(); ++ d ) {
			assert img1.dimension( d ) == img2.dimension( d ): String.format( "Mismatch in dimension %d", d );
		}
		
		assert r.length == img1.numDimensions() || r.length == 1: "Mismatch in number of dimensions and radii";
		
		this.img1 = img1;
		this.img2 = img2;
		
		this.dim = new long[ img1.numDimensions() ];
		img1.dimensions( dim );
		this.min = new long[ dim.length ];
		this.max = new long[ dim.length ];
		for ( int d = 0; d < dim.length; ++d ) {
			this.min[d] = 0;
			this.max[d] = dim[d] - 1;
		}
		
		this.correlations    = ArrayImgs.floats( dim );
		this.calculatedCheck = ArrayImgs.bits( dim );
		
		if ( r.length == 1 ) {
			this.r = new long[ this.dim.length ];
			for (int i = 0; i < this.r.length; i++) {
				this.r[ i ] = r[ 0 ];
			}
		} else		
			this.r = r.clone();
		
	}
	
	public class CrossCorrelationRandomAaccess extends Point implements RandomAccess< FloatType > {
		
		private final ArrayRandomAccess< BitType > checkAccess;
		private final ArrayRandomAccess< FloatType > correlationsAccess;
		
		private final long[] intervalMin;
		private final long[] intervalMax;
	
		private CrossCorrelationRandomAaccess(final long[] position,
				final ArrayRandomAccess<BitType> checkAccess,
				final ArrayRandomAccess<FloatType> correlationsAccess,
				final long[] intervalMin, final long[] intervalMax) {
			super(position);
			this.checkAccess = checkAccess;
			this.correlationsAccess = correlationsAccess;
			this.intervalMin = intervalMin;
			this.intervalMax = intervalMax;
		}

		public CrossCorrelationRandomAaccess() {
			super( dim.length );
			this.checkAccess        = calculatedCheck.randomAccess();
			this.correlationsAccess = correlations.randomAccess();
			
			intervalMin = new long[ dim.length ];
			intervalMax = new long[ dim.length ];
		}

		@Override
		public FloatType get() {
			checkAccess.setPosition( this.position );
			correlationsAccess.setPosition( this.position );
			
			final FloatType currVal = correlationsAccess.get();
			
			if ( checkAccess.get().get() == false ) {
				
				for ( int d = 0; d < this.n; ++d ) {
					intervalMin[d] = Math.max( 0,      this.position[d] - r[d] );
					intervalMax[d] = Math.min( max[d], this.position[d] + r[d] );
				}
				
				currVal.setReal( calculateNormalizedCrossCorrelation( 
						Views.interval( img1, intervalMin, intervalMax ),
						Views.interval( img2, intervalMin, intervalMax )
						) 
						);
			}
			
			return currVal;
			
		}

		@Override
		public CrossCorrelationRandomAaccess copy() {
			return new CrossCorrelationRandomAaccess( this.position.clone(), 
					checkAccess.copy(), 
					correlationsAccess.copy(), 
					intervalMin.clone(), 
					intervalMax.clone());
		}

		@Override
		public CrossCorrelationRandomAaccess copyRandomAccess() {
			return copy();
		}
		
	}
	
	public double calculateNormalizedCrossCorrelation( final RandomAccessibleInterval< T > i1, final RandomAccessibleInterval< U > i2 ) {
		double cc = 0.0;
		
		long nElements = 1;
		for ( int d = 0; d < i1.numDimensions(); ++d ) {
			nElements *= i1.dimension( d );
		}
		
		final double nElementsDouble = nElements;
		
		final double mean1 = calculateSum( i1 ) / nElementsDouble;
		final double mean2 = calculateSum( i2 ) / nElementsDouble;
		final double var1 = calculateSumOfSquaredDifferences( i1, mean1 ) / nElementsDouble;
		final double var2 = calculateSumOfSquaredDifferences( i2, mean2 ) / nElementsDouble;
		
		final Cursor<T> c1 = Views.flatIterable( i1 ).cursor();
		final Cursor<U> c2 = Views.flatIterable( i2 ).cursor();
		
		while( c1.hasNext() ) {
			cc += ( c1.next().getRealDouble() - mean1 ) * ( c2.next().getRealDouble() - mean2 );
		}
		
		return cc / ( Math.sqrt( var1 ) * Math.sqrt( var2 ) * nElementsDouble );
	}
	
	
	public < V extends RealType<V> > double  calculateSum( final RandomAccessibleInterval< V > i ) {
		double sum = 0.0;
		final Cursor< V > c = Views.flatIterable( i ).cursor();
		while ( c.hasNext() ) {
			sum += c.next().getRealDouble();
		}
		return sum;
	}
	
	
	public < V extends RealType<V> > double calculateSumOfSquaredDifferences( final RandomAccessibleInterval< V > i, final double mean ) {
		double sum = 0.0;
		final Cursor< V > c = Views.flatIterable( i ).cursor();
		double tmpDiff;
		while( c.hasNext() ) {
			tmpDiff = c.next().getRealDouble() - mean;
			sum    += tmpDiff*tmpDiff;
		}
		return sum;
	}


	@Override
	public CrossCorrelationRandomAaccess randomAccess() {
		return new CrossCorrelationRandomAaccess();
	}

	@Override
	public CrossCorrelationRandomAaccess randomAccess(final Interval interval) {
		return randomAccess();
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
