package org.janelia.correlations;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.BitArray;
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
public class CrossCorrelation < T extends RealType< T >, U extends RealType< U > > extends AbstractCrossCorrelation< T, U > implements RandomAccessibleInterval< FloatType > {
	
	
	
	private final ArrayImg< BitType, BitArray > calculatedCheck;
	

	public CrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r ) {
		super( img1, img2, r );
		this.calculatedCheck = ArrayImgs.bits( dim );
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

}
