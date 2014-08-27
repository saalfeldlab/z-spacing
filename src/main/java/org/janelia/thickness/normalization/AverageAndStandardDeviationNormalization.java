package org.janelia.thickness.normalization;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class AverageAndStandardDeviationNormalization extends
		AbstractColumnNormalization {

	@Override
	public <T extends RealType<T>> void normalize(
			final RandomAccessibleInterval<T> input) {
		
		final int maxDim = input.numDimensions() - 1;
		
		final Cursor<T> cursor1 = Views.flatIterable( input ).cursor();
		final Cursor<T> cursor2 = Views.flatIterable( input ).cursor();
		
		int N = 1;
		for ( int d = 0; d < input.numDimensions(); ++d ) {
			N *= input.dimension( d );
		}
		
		double mean = 0.0;
		double variance = 0.0;
		
		while ( cursor1.hasNext() ) {
			mean += cursor1.next().getRealDouble() - cursor1.getDoublePosition( maxDim );
		}
		mean /= N;
		
		while (cursor2.hasNext() ) {
			final double diff = cursor2.next().getRealDouble() - cursor1.getDoublePosition( maxDim ) - mean;
			variance += diff * diff;
		}
		variance /= ( N - 1 );
		final double stdev = Math.sqrt( variance );
		
		this.normalize( input, stdev, mean );
		
		

	}

	@Override
	public void normalize(final ListImg<double[]> input) {
		double mean = 0.0;
		double variance = 0.0;
		int N = 0;
		for ( final double[] arr : input ) {
			for (int i = 0; i < arr.length; i++) {
				mean += ( arr[i] - i );
			}
			N += arr.length;
		}
		mean /= N;
		
		for ( final double[] arr : input ) {
			for ( int i = 0; i < arr.length; ++i ) {
				final double diff = ( arr[i] - i ) - mean;
				variance += diff;
			}
		}
		variance /= ( N - 1 );
		final double stdev = Math.sqrt( variance );
		
		this.normalize( input, stdev, mean );
	}

}
