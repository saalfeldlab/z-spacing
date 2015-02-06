package org.janelia.correlations.ij;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.RealSum;

/**
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CrossCorrelationFromFormatString {
	
	// TODO Convert RGB image to gs image with NaN for each pixel for which is not true: R == G == B
	
	public static double ncc(final FloatProcessor a, final FloatProcessor b) {
        final float[] ap = (float[]) a.getPixels();
        final float[] bp = (float[])b.getPixels();
        final RealSum sumA  = new RealSum();
        final RealSum sumAA = new RealSum();
        final RealSum sumB  = new RealSum();
        final RealSum sumBB = new RealSum();
        final RealSum sumAB = new RealSum();
        int n = 0;
        for (int i = 0; i < ap.length; ++i) {
            final float va = ap[i];
            final float vb = bp[i];

            if ( Float.isNaN( va ) || Float.isNaN( vb ) )
            	continue;
            ++n;
            sumA.add( va );
            sumAA.add( va * va);
            sumB.add( vb );
            sumBB.add( vb * vb);
            sumAB.add( va * vb );
        }
        final double suma = sumA.getSum();
        final double sumaa = sumAA.getSum();
        final double sumb = sumB.getSum();
        final double sumbb = sumBB.getSum();
        final double sumab = sumAB.getSum();

        return (n * sumab - suma * sumb) / Math.sqrt(n * sumaa - suma * suma) / Math.sqrt(n * sumbb - sumb * sumb);
    }
	
	
	final static FloatProcessor openImage( final String formatString, final int z ) {
        final String filePath = String.format( formatString, z );
        final ImagePlus imp = new ImagePlus(filePath);
        return imp.getProcessor().convertToFloatProcessor();
    }
	
	
	public static RandomAccessibleInterval< FloatType > createMatrix( final String formatString, final int size, final int range ) throws InterruptedException {
		final boolean showProgress = false;
		return createMatrix(formatString, size, range, showProgress);
	}
	
	
	public static RandomAccessibleInterval< FloatType > createMatrix( final String formatString, final int size, final int range, final boolean showProgress ) throws InterruptedException {

        /* initialize matrix */
        final FloatProcessor matrix = new FloatProcessor(size, size);
        matrix.add(Double.NaN);
        for (int i = 0; i < size; ++i) {
                matrix.setf(i, i, 1.0f);
        }
        matrix.setMinAndMax(-0.2, 1);
        final ImagePlus impMatrix = new ImagePlus("inlier ratio matrix", matrix);
        if ( showProgress ) {
        	new ImageJ();
        	impMatrix.show();
        }

        /* match */
        final ArrayList<Thread> threads = new ArrayList<Thread>();
        for (int z1 = 0; z1 < size; ++z1) {
            final int fz1 = z1;
            final FloatProcessor ip1 = openImage(formatString, fz1);
            final AtomicInteger j = new AtomicInteger(fz1 + 1);
            for (int t = 0; t < Runtime.getRuntime().availableProcessors(); ++t) {
                final Thread thread = new Thread(
                    new Runnable(){
                        @Override
                        public void run(){
                            for (int fz2 = j.getAndIncrement(); fz2 < size && fz2 < fz1 + range; fz2 = j.getAndIncrement()) {
                                final FloatProcessor ip2 = openImage(formatString, fz2);
                                final float ncc = (float)ncc(ip1, ip2);
                                matrix.setf(fz1, fz2, ncc);
                                matrix.setf(fz2, fz1, ncc);
                                impMatrix.updateAndDraw();
                            }
                        }
                    }
                );
                threads.add(thread);
                thread.start();
            }
            for (final Thread t : threads)
                t.join();
        }
		return ImageJFunctions.wrapFloat( impMatrix );
	}
	
	
	public static void main(final String[] args) throws InterruptedException {
		final String formatString = "/groups/saalfeld/saalfeldlab/data/boergens/export-elastic-5/%04d.tif";
	    final int size = 100;
	    final int range = 5;
	    final RandomAccessibleInterval<FloatType> matrix = createMatrix(formatString, size, range );
	    new ImageJ();
	    ImageJFunctions.show( matrix );
	}
	
	

}
