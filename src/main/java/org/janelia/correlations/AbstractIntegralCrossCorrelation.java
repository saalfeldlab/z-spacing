/**
 * 
 */
package org.janelia.correlations;

import java.util.ArrayList;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.integral.IntegralImg;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> pixel type of first input image
 * @param <U> pixel type of second input image
 * @param <S> pixel type of output image
 * @param <I> pixel type of internally stored integral images
 */
public abstract class AbstractIntegralCrossCorrelation< T extends RealType< T >, 
U extends RealType< U >, 
S extends RealType< S > & NativeType< S >,
I extends RealType< I > > extends AbstractCrossCorrelation< T, U, S > {
	
	protected Img< I > sums1;
	protected Img< I > sums2;
	protected Img< I > sums11;
	protected Img< I > sums22;
	protected Img< I > sums12;
	protected final CrossCorrelationType type;
	
	public static class NotEnoughSpaceException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8846429713210757347L;

		
	}
	
	public enum CrossCorrelationType { STANDARD, SIGNED_SQUARED };
	
	public AbstractIntegralCrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final S n ) throws NotEnoughSpaceException {
		this(img1, img2, r, CrossCorrelationType.STANDARD, n );
	}
	
	
	public AbstractIntegralCrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final CrossCorrelationType type,
			final S n ) throws NotEnoughSpaceException {
		super( img1, img2, r, n );
		this.type = type;
	}
	
	protected void generateResultImage() throws NotEnoughSpaceException {
		switch ( type ) {
		case STANDARD:
			this.calculateCrossCorrelation();
			break;
			
		case SIGNED_SQUARED:
			this.calculateCrossCorrelationSignedSquared();
			break;

		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see net.imglib2.RandomAccessible#randomAccess()
	 */
	@Override
	public RandomAccess< S > randomAccess() {
		return this.correlations.randomAccess();
	}

	/* (non-Javadoc)
	 * @see net.imglib2.RandomAccessible#randomAccess(net.imglib2.Interval)
	 */
	@Override
	public RandomAccess< S > randomAccess(final Interval interval) {
		return randomAccess();
	}
	
	
	public static ArrayList< int[] > generateConfigurations( final int nDim ) {
		final int nCorners = (int) Math.pow( 2, nDim );
		final ArrayList<int[]> res = new ArrayList< int[] >();
		for ( int i = 0; i < nCorners; ++i ) {
			final int[] indicatorArray = new int[ nDim ];
			for ( int j = 0; j < nDim; ++j ) {
				indicatorArray[ j ] = ( ( i >> j ) & 1 ) > 0 ? 1 : 0;
			}
			res.add( indicatorArray );
		}
		return res;
	}
	
	
	private void calculateCrossCorrelation() throws NotEnoughSpaceException {
		calculateCrossCorrelationSignedSquared();
		for ( final S c : this.correlations ) {
			final double val = c.getRealDouble();
			c.setReal( (val < 0 ? -Math.sqrt( -val ) : Math.sqrt( val ) ) );
		}

		
	}

	
	private void calculateCrossCorrelationSignedSquared() throws NotEnoughSpaceException {
		calculateIntegralImages();
		final RandomAccess< I > ra1  = sums1.randomAccess();
		final RandomAccess< I > ra2  = sums2.randomAccess();
		final RandomAccess< I > ra11 = sums11.randomAccess();
		final RandomAccess< I > ra12 = sums12.randomAccess();
		final RandomAccess< I > ra22 = sums22.randomAccess();
		
		final ArrayCursor< S > c = this.correlations.cursor();
		
		final long[] lower = new long[ this.dim.length ];
		final long[] upper = new long[ this.dim.length ];
		final long[][] lu  = new long[][] { lower, upper };
		final long[] shiftedRadius = this.r.clone();
		for (int i = 0; i < shiftedRadius.length; i++) {
			shiftedRadius[i] += 1;
		}
		final ArrayList<int[]> configurations = generateConfigurations( this.dim.length );
		
		// radius in backwards direction needs to be longer by 1
		// need to grab the first pixel outside rectangle
		final long[] backRad = this.r.clone();
		for (int i = 0; i < backRad.length; i++) {
			backRad[ i ] += 1;
		}
		
		while( c.hasNext() ) {
			
			c.fwd();
			
			int nPoints = 1;
			for (int d = 0; d < upper.length; ++d ) {
				final long currPos = c.getLongPosition( d ) + 1;
				lower[ d ] = Math.max( currPos - backRad[ d ], 0 );
				upper[ d ] = Math.min( currPos + this.r[ d ], this.dim[ d ] );
				nPoints *= upper[ d ] - lower [ d ];
			}
			final double nPointsDouble = nPoints;
			
			double val = 0.0;
			
			double sum1  = 0.0;
			double sum2  = 0.0;
			double sum11 = 0.0;
			double sum12 = 0.0;
			double sum22 = 0.0;
			
			
			for ( final int[] p : configurations ) {
				int sum = this.dim.length;
				for ( int d = 0; d < p.length; ++d ) {
					final long pos = lu[ p[d] ][ d ];
					ra1. setPosition( pos, d );
					ra2. setPosition( pos, d );
					ra11.setPosition( pos, d );
					ra12.setPosition( pos, d );
					ra22.setPosition( pos, d );
					sum -= p[d];
				}
				// sign = (-1)^ ( nDimensions - || p ||_1 )
				// as p_i = { 0, 1 }, || p ||_1 = sum of non-zero elements
				final double sign = ( (sum & 1) == 0 ) ? 1. : -1. ;
				sum1  += sign * ra1. get().getRealDouble();
				sum2  += sign * ra2. get().getRealDouble();
				sum11 += sign * ra11.get().getRealDouble();
				sum12 += sign * ra12.get().getRealDouble();
				sum22 += sign * ra22.get().getRealDouble();
			}
			val  = ( nPointsDouble * sum12 - sum1*sum2 );
			val *= ( val > 0 ? val : -val );
			val /= ( ( nPointsDouble * sum11 - sum1*sum1 ) * ( nPointsDouble * sum22 - sum2*sum2 ) );
			
			c.get().setReal( val );
			
			
		}
	}
	
	
	
	protected abstract void calculateIntegralImages() throws NotEnoughSpaceException;
	
	public static < V extends RealType< V >, W extends RealType< W > & NativeType< W > > Img< W > generateIntegralImageFromSource( final RandomAccessibleInterval< V > input, final Converter< V, W > converter, final W dummy ) throws NotEnoughSpaceException {
		final IntegralImg< V, W > ii = new IntegralImg< V, W >( input, dummy, converter );
		
		if ( ! ii.process() )
			throw new NotEnoughSpaceException();
		
		return ii.getResult();
	}

}
