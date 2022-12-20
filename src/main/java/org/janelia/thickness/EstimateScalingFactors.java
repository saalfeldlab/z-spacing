/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.janelia.thickness;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class EstimateScalingFactors
{

	public static < T extends RealType< T >, W extends RealType< W > > void estimateQuadraticFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] scalingFactors,
			final double[] coordinates,
			final RandomAccessibleInterval< double[] > localFits,
			final double regularizerWeight,
			final int comparisonRange,
			final int nIterations,
			final RandomAccessibleInterval< W > pairwiseWeights )
	{

		final double inverseRegularizerWeight = 1 - regularizerWeight;

		final RandomAccess< T > corrAccess = correlations.randomAccess();
		final RandomAccess< W > wAccess = pairwiseWeights.randomAccess();

		for ( int iter = 0; iter < nIterations; ++iter )
		{

			final Cursor< double[] > fitCursor = Views.iterable( localFits ).cursor();

			for ( int n = 0; fitCursor.hasNext(); ++n )
			{

				// is this allocation expensive? should this occur one loop
				// further outside?
				final double[] oldScalingFactors = scalingFactors.clone();

				corrAccess.setPosition( n, 0 );
				wAccess.setPosition( n, 0 );

				final double[] lf = fitCursor.next();
				final RealRandomAccessible< DoubleType > interpolatedFit = Views.interpolate( Views.extendValue( ArrayImgs.doubles( lf, lf.length ), new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory< DoubleType >() );
				final RealRandomAccess< DoubleType > ra = interpolatedFit.realRandomAccess();
				double enumeratorSum = 0.0;
				double denominatorSum = 0.0;
				final int minVal = Math.max( n - comparisonRange, 0 );
				final int maxVal = Math.min( n + comparisonRange, scalingFactors.length );
				for ( int i = minVal; i < maxVal; ++i )
				{
					if ( i == n )
						continue;
					corrAccess.setPosition( i, 1 );
					wAccess.setPosition( i, 1 );
					ra.setPosition( Math.abs( coordinates[ i ] - coordinates[ n ] ), 0 );
					// fits are negative because LUTRealtransform requires
					// increasing function
					final double fitVal = -ra.get().get();
					final double measure = corrAccess.get().getRealDouble();
					if ( Double.isNaN( fitVal ) || Double.isNaN( measure ) || measure <= 0.0 )
						continue;
					final double w = wAccess.get().getRealDouble();
					final double prod = oldScalingFactors[ i ] * measure;
					final double h = w * prod;

					enumeratorSum += h * fitVal;
					denominatorSum += h * prod;
				}
				final double result = enumeratorSum / denominatorSum * inverseRegularizerWeight + regularizerWeight * oldScalingFactors[ n ];
				if ( !Double.isNaN( result ) )
					scalingFactors[ n ] = result;
			}

		}
	}

}
