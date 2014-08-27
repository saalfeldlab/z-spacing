package org.janelia.thickness.normalization;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public abstract class AbstractColumnNormalization implements
		NormalizationInterface {
	
	public <T extends RealType<T>> void normalize(
			final RandomAccessibleInterval<T> input,
			final double scalingFactor,
			final double referenceShift ) {
		for ( int y = 0; y < input.dimension( 1 ); ++y ) {
			final IntervalView<T> xzPlane = Views.hyperSlice( input, 1, y );
			for ( int x = 0; x < input.dimension( 0 ); ++x ) {
				final Cursor<T> cursor = Views.flatIterable( Views.hyperSlice( xzPlane, 0, x ) ).cursor();
				final double baseVal = cursor.next().getRealDouble();
				while( cursor.hasNext() ) {
					final T current = cursor.get();
					// + baseVal or + refernceShift or + baseVal - refernceShift or what?
					current.setReal( ( current.getRealDouble() )*scalingFactor  );
					cursor.fwd();
				}
			}
		}
	}
	
	public void normalize(
			final ListImg< double[] > input,
			final double scalingFactor,
			final double referenceShift ) {
		for ( final double[] arr : input ) {
			for (int i = 0; i < arr.length; i++) {
				arr[ i ] = arr[ i ] * scalingFactor; 
			}
		}
	}

	

}
