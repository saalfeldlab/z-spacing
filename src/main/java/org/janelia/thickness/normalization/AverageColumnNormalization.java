package org.janelia.thickness.normalization;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class AverageColumnNormalization extends AbstractColumnNormalization {

	@Override
	public <T extends RealType<T>> void normalize(
			final RandomAccessibleInterval<T> input) {
		final IntervalView<T> minSlice = Views.hyperSlice( input, 2, 0 );
		final IntervalView<T> maxSlice = Views.hyperSlice( input, 2, input.dimension( 2 ) - 1 );
		
		final Cursor<T> minCursor = Views.flatIterable( minSlice ).cursor();
		final Cursor<T> maxCursor = Views.flatIterable( maxSlice ).cursor();
		
		double avgDiff  = 0;
		double avgShift = 0;
		
		while( minCursor.hasNext() ) {
			final double currMax = maxCursor.next().getRealDouble();
			final double currMin = minCursor.next().getRealDouble();
			avgDiff  += currMax - currMin;
			avgShift += currMin;
		}
		
		final long mult = minSlice.dimension( 0 ) * minSlice.dimension( 1 );
		
		avgDiff  /= mult;
		avgShift /= mult;
		
		if ( avgDiff == 0 )
			return;
		
		final double scalingFactor = input.dimension( 2 ) / avgDiff;
		
		this.normalize( input, scalingFactor, avgShift );
	}

	@Override
	public void normalize(final ListImg<double[]> input) {
		double avgShift = 0.0;
		double avgScalingFactor = 0.0;
		int N = 1;
		for ( int d = 0; d < input.numDimensions(); ++d ) {
			N *= input.dimension(d);
		}
		for ( final double[] arr : input ) {
			final double currMax = arr[ arr.length - 1 ];
			final double currMin = arr[ 0 ];
			avgShift += currMin;
			avgScalingFactor += arr.length / ( currMax - currMin );
		}
		avgShift /= N;
		avgScalingFactor /= 10;
		
		
		this.normalize( input, avgScalingFactor, avgShift );
	}

}
