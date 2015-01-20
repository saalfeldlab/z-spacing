/**
 * 
 */
package org.janelia.correlations;

import ij.process.FloatProcessor;

import java.util.Random;

import mpicbg.ij.integral.BlockPMCC;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.utility.converter.SameTypeConverter;


/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> pixel type of first input image
 * @param <U> pixel type of second input image
 * @param <S> pixel type of output image
 * @param <I> pixel type of internally stored integral images
 */
public class IntegralCrossCorrelation< T extends RealType< T >, U extends RealType< U >, S extends RealType< S > & NativeType< S >, I extends RealType< I > & NativeType< I > > extends
		AbstractIntegralCrossCorrelation<T, U, S, I > {
	
	protected final Converter< T, I > converterT;
	protected final Converter< U, I > converterU;
	protected final I iiDummy;
	
	public IntegralCrossCorrelation(
			final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final Converter< T, I > converterT, 
			final Converter< U, I > converterU,
			final S resultDummy,
			final I iiDummy
			)
			throws org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException {
		this(img1, img2, r, CrossCorrelationType.STANDARD, converterT, converterU, resultDummy, iiDummy );
	}
	
	public IntegralCrossCorrelation(
			final RandomAccessibleInterval<T> img1,
			final RandomAccessibleInterval<U> img2,
			final long[] r,
			final CrossCorrelationType type,
			final Converter< T, I > converterT, 
			final Converter< U, I > converterU,
			final S resultDummy,
			final I iiDummy
			)
			throws org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException {
		super(img1, img2, r, type, resultDummy);
		this.converterT = converterT;
		this.converterU = converterU;
		this.iiDummy = iiDummy;
		this.generateResultImage( true );
	}

	@Override
	protected void calculateIntegralImages() throws NotEnoughSpaceException {
		
		final ArrayImg< I, ? > squares1 = new ArrayImgFactory< I >().create( this.dim, iiDummy );
		final ArrayImg< I, ? > squares2 = new ArrayImgFactory< I >().create( this.dim, iiDummy );
		final ArrayImg< I,  ?> mult12   = new ArrayImgFactory< I >().create( this.dim, iiDummy );
		
		final ArrayCursor< I > cs1  = squares1.cursor();
		final ArrayCursor< I > cs2  = squares2.cursor();
		final ArrayCursor< I > cm12 = mult12.cursor();
		
		final Cursor<T> c1 = Views.flatIterable( img1 ).cursor();
		final Cursor<U> c2 = Views.flatIterable( img2 ).cursor();
		
		while ( c1.hasNext() ) {
			final T val1 = c1.next();
			final U val2 = c2.next();
			final I cs1Val  = cs1.next();
			final I cs2Val  = cs2.next();
			final I cm12Val = cm12.next();
			
			converterT.convert( val1, cs1Val );
			converterU.convert( val2, cs2Val );
			converterT.convert( val1, cm12Val );
			converterU.convert( val2, iiDummy );
			
			cs1Val.mul( cs1Val );
			cs2Val.mul( cs2Val );
			cm12Val.mul( iiDummy );
			
		}
		
		sums1  = generateIntegralImageFromSource( img1, converterT, iiDummy.copy() );
		sums2  = generateIntegralImageFromSource( img2, converterU, iiDummy.copy() );
		sums11 = generateIntegralImageFromSource( squares1, new SameTypeConverter< I >(), iiDummy.copy() );
		sums22 = generateIntegralImageFromSource( squares2, new SameTypeConverter< I >(), iiDummy.copy() );
		sums12 = generateIntegralImageFromSource( mult12, new SameTypeConverter< I >(), iiDummy.copy() );
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
		
		final IntegralCrossCorrelation< FloatType, FloatType, FloatType, FloatType > ii = 
				new IntegralCrossCorrelation< FloatType, FloatType, FloatType, FloatType >(img1, 
						img2,  
						new long[] { rad[0], rad[1] }, 
						new SameTypeConverter<FloatType>(),
						new SameTypeConverter<FloatType>(),
						new FloatType( 0.0f ),
						new FloatType( 0.0f ) );
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
