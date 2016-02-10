package org.janelia.thickness;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class EstimateQualityOfSlice {

	public static < T extends RealType< T > > void estimateQuadraticFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] weights,
			final double[] multipliers,
			final double[] coordinates,
			final ListImg< double[] > localFits,
			final double regularizerWeight,
			final int comparisonRange,
			final int nIterations ) {

		final double inverseRegularizerWeight = 1 - regularizerWeight;

		final RandomAccess< T > corrAccess = correlations.randomAccess();

		for ( int iter = 0; iter < nIterations; ++iter ) {

			final ListCursor<double[]> fitCursor = localFits.cursor();

			for ( int n = 0; fitCursor.hasNext(); ++n ) {

				// is this allocation expensive?
				final double[] oldMultipliers = multipliers.clone();

				corrAccess.setPosition( n, 0 );

				final double[] lf = fitCursor.next();
				final RealRandomAccessible<DoubleType> interpolatedFit = Views.interpolate( Views.extendValue( ArrayImgs.doubles( lf, lf.length ), new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>() );
				final RealRandomAccess<DoubleType> ra = interpolatedFit.realRandomAccess();
				double enumeratorSum  = 0.0;
				double denominatorSum = 0.0;
				final int minVal = Math.max( n - comparisonRange, 0 );
				final int maxVal = Math.min( n + comparisonRange, multipliers.length );
				for ( int i = minVal; i < maxVal; ++i ) {
					if ( i == n )
						continue;
					corrAccess.setPosition( i, 1 );
					ra.setPosition( Math.abs( coordinates[ i ] - coordinates[ n ] ), 0 );
					// fits are negative because LUTRealtransform requires increasing function
					final double fitVal  = -ra.get().get();
					final double measure = corrAccess.get().getRealDouble();
					if ( Double.isNaN( fitVal ) || Double.isNaN( measure ) || measure <= 0.0  )
						continue;
					final double prod = oldMultipliers[ i ] * measure;
					enumeratorSum  += prod * fitVal;
					denominatorSum += prod * prod;
				}
				final double result = enumeratorSum / denominatorSum * inverseRegularizerWeight + regularizerWeight;
				if ( ! Double.isNaN( result ) )
					multipliers[ n ] = result;
			}

		}
	}


}
