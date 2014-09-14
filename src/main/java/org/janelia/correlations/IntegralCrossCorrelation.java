/**
 * 
 */
package org.janelia.correlations;

import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.Random;

import mpicbg.ij.integral.BlockPMCC;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.integral.IntegralImg;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class IntegralCrossCorrelation< T extends RealType< T >, U extends RealType< U >, S extends RealType< S > & NativeType< S > > extends AbstractCrossCorrelation< T, U, S > {
	
	private Img< FloatType > sums1;
	private Img< FloatType > sums2;
	private Img< FloatType > sums11;
	private Img< FloatType > sums22;
	private Img< FloatType > sums12;
	private final FloatType dummy = new FloatType();
	
	public static class NotEnoughSpaceException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8846429713210757347L;

		
	}
	
	public enum CrossCorrelationType { STANDARD, SIGNED_SQUARED };
	
	public IntegralCrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final S n ) throws NotEnoughSpaceException {
		this(img1, img2, r, CrossCorrelationType.STANDARD, n );
	}
	
	
	public IntegralCrossCorrelation(final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final CrossCorrelationType type,
			final S n ) throws NotEnoughSpaceException {
		super( img1, img2, r, n );
		
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
		final RandomAccess<FloatType> ra1  = sums1.randomAccess();
		final RandomAccess<FloatType> ra2  = sums2.randomAccess();
		final RandomAccess<FloatType> ra11 = sums11.randomAccess();
		final RandomAccess<FloatType> ra12 = sums12.randomAccess();
		final RandomAccess<FloatType> ra22 = sums22.randomAccess();
		
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
				sum1  += sign * ra1. get().get();
				sum2  += sign * ra2. get().get();
				sum11 += sign * ra11.get().get();
				sum12 += sign * ra12.get().get();
				sum22 += sign * ra22.get().get();
			}
			val  = ( nPointsDouble * sum12 - sum1*sum2 );
			val *= ( val > 0 ? val : -val );
			val /= ( ( nPointsDouble * sum11 - sum1*sum1 ) * ( nPointsDouble * sum22 - sum2*sum2 ) );
			
			c.get().setReal( val );
			
			
		}
	}
	
	
	
	private void calculateIntegralImages() throws NotEnoughSpaceException {
		
		final ArrayImg<FloatType, FloatArray> squares1 = ArrayImgs.floats( this.dim );
		final ArrayImg<FloatType, FloatArray> squares2 = ArrayImgs.floats( this.dim );
		final ArrayImg<FloatType, FloatArray> mult12   = ArrayImgs.floats( this.dim );
		
		final ArrayCursor<FloatType> cs1  = squares1.cursor();
		final ArrayCursor<FloatType> cs2  = squares2.cursor();
		final ArrayCursor<FloatType> cm12 = mult12.cursor();
		
		final Cursor<T> c1 = Views.flatIterable( img1 ).cursor();
		final Cursor<U> c2 = Views.flatIterable( img2 ).cursor();
		
		while ( c1.hasNext() ) {
			final float val1 = c1.next().getRealFloat();
			final float val2 = c2.next().getRealFloat();
			cs1. next().set( val1*val1 );
			cs2. next().set( val2*val2 );
			cm12.next().set( val1*val2 );
		}
		
		sums1  = generateIntegralImageFromSource( img1 );
		sums2  = generateIntegralImageFromSource( img2 );
		sums11 = generateIntegralImageFromSource( squares1 );
		sums22 = generateIntegralImageFromSource( squares2 );
		sums12 = generateIntegralImageFromSource( mult12 );
	}
	
	public < V extends RealType< V > > Img< FloatType > generateIntegralImageFromSource( final RandomAccessibleInterval< V > input ) throws NotEnoughSpaceException {
		final RealFloatConverter<V> c = new RealFloatConverter< V >();
		final IntegralImg<V, FloatType> ii = new IntegralImg<V, FloatType>( input, dummy, c );
		
		if ( ! ii.process() )
			throw new NotEnoughSpaceException();
		
		return ii.getResult();
	}
	
	
	public static void main(final String[] args) throws NotEnoughSpaceException {
		final int width = 10;
		final int height = 10;
		final int n = width*height;
		final int[] rad = new int[] { 3, 3 };
		final Random rng = new Random( 100 );
		final float[] r = new float[ n ];
		final float[] s = new float[ n ];
		for (int i = 0; i < r.length; i++) {
			r[i] = rng.nextFloat();
			s[i] = rng.nextFloat();
		}
		
		final float[] range = new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
		
		final ArrayImg<FloatType, FloatArray> img1 = ArrayImgs.floats( r, width, height );
		final ArrayImg<FloatType, FloatArray> img2 = ArrayImgs.floats( s, width, height );
		
		final FloatProcessor fpR = new FloatProcessor( new float[width][height] );
		final FloatProcessor fpS = new FloatProcessor( new float[width][height] );
		
		final ArrayCursor<FloatType> c1 = img1.cursor();
		final ArrayCursor<FloatType> c2 = img2.cursor();
		
		while ( c1.hasNext() ) {
			c1.fwd();
			c2.fwd();
			fpR.setf( c1.getIntPosition( 0 ), c1.getIntPosition( 1 ), c1.get().get() );
			fpS.setf( c2.getIntPosition( 0 ), c2.getIntPosition( 1 ), c2.get().get() );
		}
		
		final ArrayImg<FloatType, FloatArray> imgr = ArrayImgs.floats( range, 5, 3 );
		
//		new ImageJ();
		
		final IntegralCrossCorrelation< FloatType, FloatType, FloatType > ii = new IntegralCrossCorrelation< FloatType, FloatType, FloatType >(img1, img2,  new long[] { rad[0], rad[1] }, new FloatType() );
		final BlockPMCC cc = new BlockPMCC(fpR, fpS, 0, 0);
		cc.r( rad[0], rad[1] );
		final FloatProcessor fp = cc.getTargetProcessor();
		
//		ImageJFunctions.show( ii );
//		new ImagePlus( "deprecated", fp ).show();
		
		final float tolerance = 1e-5f;
		final Cursor<FloatType> i = Views.flatIterable( ii ).cursor();
		while ( i.hasNext() ) {
			i.fwd();
			if ( Math.abs( i.get().get() - fp.getf( i.getIntPosition( 0 ), i.getIntPosition( 1 ) ) ) > tolerance )
					System.out.println( "Diff greater than " + tolerance + "!" );
		}
		
		
		
	}

}
