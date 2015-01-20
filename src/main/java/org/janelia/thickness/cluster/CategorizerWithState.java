/**
 * 
 */
package org.janelia.thickness.cluster;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.List;

import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.fft2.FFT;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException;
import org.janelia.correlations.CrossCorrelation;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CategorizerWithState implements Categorizer {
	
	List< Categorizer > categorizers;
	private Categorizer currentCategorizer;

	
	/**
	 * @param categorizers
	 * @param currentCategorizer
	 */
	public CategorizerWithState(final List<Categorizer> categorizers ) {
		super();
		this.categorizers = categorizers;
		if ( this.categorizers.size() > 0 )
			this.currentCategorizer = this.categorizers.get( 0 );
		else
			this.currentCategorizer = null;
	}
	

	@Override
	public double[][] getLabels( final double[] coordinates ) {
		return this.currentCategorizer.getLabels( coordinates );
	}

	
	@Override
	public <T extends RealType<T>> double[][] getLabels(
			final RandomAccessibleInterval<T> strip) {
		return this.currentCategorizer.getLabels( strip );
	}

	
	@Override
	public void setState(final int n) {
		this.currentCategorizer = categorizers.get( n );
	}
	
	
	public static < T extends RealType< T > > CategorizerWithState 
	generateFixedRange( final int[] ranges ) {
		final List< Categorizer > list = new ArrayList<Categorizer>();
		for ( int i = 0; i < ranges.length; ++i ) {
			final int range = ranges[i];
			final RangedCategorizer cat = new RangedCategorizer( range );
			list.add( cat );
		}
		final CategorizerWithState result = new CategorizerWithState( list );
		return result;
	}
	
	
	public static < T extends RealType< T > > CategorizerWithState 
	generate( 
			final List< RandomAccessibleInterval< T > > images,
			final int[][] radii,
			final int dimension,
			final int range
			) throws NotEnoughDataPointsException, NotEnoughSpaceException {

		final int origNumDimensions = images.get( 0 ).numDimensions();
		final long[] originalDim   = new long[ origNumDimensions ];
		final long[] dim           = new long[ origNumDimensions - 1 ];
		final long[][] radiiExcept = new long[ radii.length ][ radii[0].length - 1 ];
		
		
		// get subview on radii and dimensions
		for ( int index1=0,index2=0; index2 < origNumDimensions; ++index2 ) {
			final long d = images.get( 0 ).dimension( index2 );
			originalDim[index2] = d;
			if ( index2 == dimension ) 
				continue;
			dim[index1] = d;
			for ( int k = 0; k < radii.length; ++k )
				radiiExcept[k][index1] = radii[k][index2];
			++index1;
		}
		
		final List< Categorizer > categorizers = new ArrayList<Categorizer>();
		
		final long limit = originalDim[dimension] - range;
		
		for ( int i = 0; i < radiiExcept.length; ++i ) {
			final long[] intervalMin = originalDim.clone();
			final long[] intervalMax = originalDim.clone();
			final int[] radius = radii[i];
			for (int j = 0; j < radius.length; j++) {
				if ( j == dimension ) {
					intervalMin[j]  = 0;
					intervalMax[j]   = originalDim[j];
				} else {
					final long val = radius[j];
					intervalMin[j]  = intervalMin[j]/2 - val;
					intervalMax[j]  = intervalMax[j]/2 + val;
				}
			}
			
			final ArrayImg<DoubleType, DoubleArray> correlationCurves = ArrayImgs.doubles( originalDim[dimension]-range, range );
			final ArrayRandomAccess<DoubleType> targetRa = correlationCurves.randomAccess();
			
			for (final RandomAccessibleInterval<T> image : images ) {
				
				for ( int xRef = 0; xRef < limit; ++xRef ) {
					
					targetRa.setPosition( xRef, 0 );
					
					final int corrLimit = xRef + range;
					final IntervalView<T> ref             = Views.hyperSlice( image, dimension, xRef );
					final IntervalView<DoubleType> target = Views.hyperSlice( correlationCurves, 0, xRef );
					final Cursor<DoubleType> t = Views.flatIterable( target ).cursor();
					for ( int x = xRef; x < corrLimit; ++x ) {
						targetRa.setPosition( x - xRef, 1 );
						final IntervalView<T> comp = Views.hyperSlice( image, dimension, x );
						final CrossCorrelation<T, T, DoubleType> cc = new CrossCorrelation<T, T, DoubleType>( ref, comp, radiiExcept[i], CrossCorrelation.TYPE.SIGNED_SQUARED, new DoubleType() );
						
						final RandomAccess<DoubleType> ccRa = cc.randomAccess();
						ccRa.setPosition( radiiExcept[i] );
//						final double val = Math.max( 0.0, ccRa.get().get() );
						final double val = ccRa.get().get();
//						t.next().add( new DoubleType( val ) );
						targetRa.get().add( new DoubleType( val ) );
					}
				}
			}
			
			final int width = 0;
			for ( final DoubleType c : correlationCurves )
				c.mul( 1.0/images.size() );
			
			ImageJFunctions.show( correlationCurves );
//			IJ.run( "FFT" );
			final ExtendedRandomAccessibleInterval<DoubleType, ArrayImg<DoubleType, DoubleArray>> mirrored = Views.extendMirrorSingle( correlationCurves );
			final Img<ComplexFloatType> fft = FFT.realToComplex( correlationCurves, new ArrayImgFactory<ComplexFloatType>());
			final Img<ComplexFloatType> ffft = FFT.realToComplex( mirrored, correlationCurves, new ArrayImgFactory<ComplexFloatType>() , new ComplexFloatType() );
			double meanX = 0.0;
			double weightSum = 0.0;
			for ( final Cursor<ComplexFloatType> f = Views.flatIterable( ffft ).cursor(); f.hasNext(); ) {
				f.fwd();
				final double x = f.getDoublePosition( 0 );
				final double w = f.get().getPowerDouble();
				meanX     += w*x;
				weightSum += w;
			}
			meanX /= weightSum;
			double varX = 0.0;
			for ( final Cursor<ComplexFloatType> f = Views.flatIterable( ffft ).cursor(); f.hasNext(); ) {
				f.fwd();
				final double x = f.getDoublePosition( 0 );
				final double w = f.get().getRealDouble();
				final double diff = x - meanX;
				varX += w*diff*diff;
			}
			varX /= weightSum;
			System.out.println( "meanX=" + meanX + ", varX=" + varX );
			ImageJFunctions.show( fft );
			ImageJFunctions.show( ffft );
//			final FourierTransform<DoubleType, ComplexDoubleType> a = new FourierTransform< DoubleType, ComplexDoubleType>( correlationCurves, new ArrayImgFactory<ComplexDoubleType>(), new ComplexDoubleType());
//			a.process();
//			final Img<ComplexDoubleType> fftResult = a.getResult();
//			ImageJFunctions.show( fftResult );
			categorizers.add( new RangedCategorizer( width ) );
		}
		
		return new CategorizerWithState( categorizers );
	}
	
	
	
	public static void main(final String[] args) throws NotEnoughDataPointsException, NotEnoughSpaceException {
		new ImageJ();
		IJ.run("FFT Options...", "fft raw complex");
		IJ.run("FFT Options...", "fft raw complex");
		// String fn = "/data/hanslovskyp/forPhilipp/substacks/03/data/data0000.tif";
		// final String fn = "/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/data/data0000.tif";
		final String root = "/data/hanslovskyp/davi_toy_set";
		final int nImages = 1;
//		final String fn = "/groups/saalfeld/home/hanslovskyp/singleSection.tif";
		IJ.run( "Image Sequence...", String.format( "open=%s/data number=%d sort", root, nImages ) );
		final ImagePlus imp = IJ.getImage();
		imp.show();
//		IJ.run("Smooth");
//		IJ.run("Smooth");
//		IJ.run("Smooth");
//		IJ.run("Smooth");
//		IJ.run("Smooth");
//		IJ.run("Smooth");
//		imp.show();
		final ArrayList<RandomAccessibleInterval<FloatType>> images = new ArrayList<RandomAccessibleInterval< FloatType > >();
		
		final ImageStack stack = imp.getStack();
		for ( int i = 1; i <= stack.getSize(); ++i ) {
			final FloatProcessor ip = stack.getProcessor( i ).convertToFloatProcessor();
			final ImagePlus currImp = new ImagePlus( "", ip );
			final Img<FloatType> image = ImageJFunctions.wrap( currImp );
			images.add( image );
		}
		
//		ImageJFunctions.show( image );
		
		final int[][] radii = new int[][] { { 5, 5 }, { 72, 72 }, { 500, 500 } };
		final int range = 10;

		CategorizerWithState.generate( images, radii, 0, range );
		
	}

}
