package org.janelia.correlations;

import ij.process.FloatProcessor;

import java.util.Random;

import mpicbg.ij.integral.BlockPMCC;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.utility.RealLongConverter;



/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> pixel type of first input image
 * @param <U> pixel type of second input image
 * @param <S> pixel type of output image
 */
public class IntegerIntegralCrossCorrelationFromIntegral< T extends RealType< T >, U extends RealType< U >, S extends RealType< S > & NativeType< S > > extends
		IntegralCrossCorrelation<T, U, S, LongType > {
	
	public IntegerIntegralCrossCorrelationFromIntegral(
			final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final S resultDummy
			)
			throws NotEnoughSpaceException {
		this( img1, img2, r, CrossCorrelationType.STANDARD, resultDummy );
	}
	
	public IntegerIntegralCrossCorrelationFromIntegral(
			final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final CrossCorrelationType type,
			final S resultDummy	
			)
			throws NotEnoughSpaceException {
		super(img1, img2, r, type, new RealLongConverter< T >(), new RealLongConverter< U >(), resultDummy, new LongType() );
	}
	
	
	
	public static void main(final String[] args) throws NotEnoughSpaceException {
		final int width = 10;
		final int height = 10;
		final int n = width*height;
		final int[] rad = new int[] { 3, 3 };
		final Random rng = new Random( 100 );
		final long[] r = new long[ n ];
		final long[] s = new long[ n ];
		for (int i = 0; i < r.length; i++) {
			r[i] = (long) (rng.nextDouble() * 1000);
			s[i] = (long) (rng.nextDouble() * 1000);
		}
		
		final float[] range = new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
		
		final ArrayImg< LongType, LongArray > img1 = ArrayImgs.longs( r, width, height );
		final ArrayImg< LongType, LongArray > img2 = ArrayImgs.longs( s, width, height );
		
		final FloatProcessor fpR = new FloatProcessor( new float[width][height] );
		final FloatProcessor fpS = new FloatProcessor( new float[width][height] );
		
		final ArrayCursor< LongType > c1 = img1.cursor();
		final ArrayCursor< LongType > c2 = img2.cursor();
		
		while ( c1.hasNext() ) {
			c1.fwd();
			c2.fwd();
			fpR.setf( c1.getIntPosition( 0 ), c1.getIntPosition( 1 ), c1.get().get() );
			fpS.setf( c2.getIntPosition( 0 ), c2.getIntPosition( 1 ), c2.get().get() );
		}
		
		final ArrayImg<FloatType, FloatArray> imgr = ArrayImgs.floats( range, 5, 3 );
		
//		new ImageJ();
		
//		final IntegralCrossCorrelation< FloatType, FloatType, FloatType, FloatType > ii = 
//				new IntegralCrossCorrelation< FloatType, FloatType, FloatType, FloatType >(img1, 
//						img2,  
//						new long[] { rad[0], rad[1] }, 
//						new SameTypeConverter<FloatType>(),
//						new SameTypeConverter<FloatType>(),
//						new FloatType( 0.0f ),
//						new FloatType( 0.0f ) );
		final IntegerIntegralCrossCorrelationFromIntegral< LongType	, LongType, FloatType > ii = 
				new IntegerIntegralCrossCorrelationFromIntegral< LongType, LongType, FloatType >( img1, img2, new long[] { rad[0], rad[1] }, new FloatType() );
		final BlockPMCC cc = new BlockPMCC(fpR, fpS, 0, 0);
		cc.r( rad[0], rad[1] );
		final FloatProcessor fp = cc.getTargetProcessor();
		
//		ImageJFunctions.show( ii );
//		new ImagePlus( "deprecated", fp ).show();
		
		final float tolerance = 1e-7f;
		final Cursor< FloatType > i = Views.flatIterable( ii ).cursor();
		while ( i.hasNext() ) {
			i.fwd();
			if ( Math.abs( i.get().get() - fp.getf( i.getIntPosition( 0 ), i.getIntPosition( 1 ) ) ) > tolerance )
					System.out.println( "Diff greater than " + tolerance + "!" );
		}
		
		
		
	}

}
