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

import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.LUTRealTransform;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ShiftCoordinates
{

	public static < T extends RealType< T > > void collectShiftsFromMatrix(
			final double[] coordinates,
			final RandomAccessibleInterval< T > correlations,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > localFits,
			final double[] shiftsArray,
			final double[] weightSums,
			final double[] shiftWeights,
			final Options options )
	{

		final int stride = 2 * options.comparisonRange;

		final RandomAccess< T > corrAccess1 = correlations.randomAccess();
		final RandomAccess< T > corrAccess2 = correlations.randomAccess();

		final double[] reference = new double[ 1 ];

		final Cursor< double[] > cursor = Views.iterable( localFits ).cursor();

		// i is reference index, k is comparison index
		final long width = correlations.dimension( 0 );
		final long height = correlations.dimension( 1 );
		for ( int i = 0; i < height; ++i )
		{

			corrAccess1.setPosition( i, 1 );
			corrAccess2.setPosition( i, 1 );
			final double[] localFit = cursor.next();
			final LUTRealTransform lut = new LUTRealTransform( localFit, 1, 1 );

			double minMeasurement1 = Double.MAX_VALUE;
			double minMeasurement2 = Double.MAX_VALUE;

			// start at 1 to avoid using values on diagonal
			final int startDist = 1;
			final double w = shiftWeights[ i ];
			for ( int dist = startDist, up = i + startDist, down = i - startDist; dist <= options.comparisonRange; ++dist, ++up, --down )
			{

				if ( up < width )
				{
					corrAccess1.setPosition( up, 0 );

					final double measurement = corrAccess1.get().getRealDouble();
					if ( Double.isNaN( measurement ) || measurement <= options.minimumCorrelationValue || options.forceMonotonicity && measurement >= minMeasurement1 )
					{

					}
					else
					{

						minMeasurement1 = measurement;
						/*
						 * TODO inverts because LUTRealTransform can only
						 * increasing
						 */
						reference[ 0 ] = -measurement;

						lut.applyInverse( reference, reference );

						// reference[0] > halfRange || ??
						if ( Double.isFinite( reference[ 0 ] ) && !( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE ) )
						{

							// rel: negative coordinates of k wrt to local
							// coordinate system of i
							final double rel = coordinates[ i ] - coordinates[ up ];

							/* current location */
							final double shift = up < i ? rel - reference[ 0 ] : rel + reference[ 0 ];
							shiftsArray[ up ] += shift * w;
							weightSums[ up ] += w;
						}
					}
				}

				if ( down >= 0 )
				{
					corrAccess2.setPosition( down, 0 );

					final double measurement = corrAccess2.get().getRealDouble();
					if ( Double.isFinite( reference[ 0 ] ) && Double.isNaN( measurement ) || measurement <= options.minimumCorrelationValue || options.forceMonotonicity && measurement >= minMeasurement2 )
					{

					}
					else
					{

						minMeasurement2 = measurement;
						/*
						 * TODO inverts because LUTRealTransform can only
						 * increasing
						 */
						reference[ 0 ] = -measurement;

						lut.applyInverse( reference, reference );

						// reference[0] > halfRange || ??
						if ( !( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE ) )
						{

							// rel: negative coordinates of k wrt to local
							// coordinate system of i
							final double rel = coordinates[ i ] - coordinates[ down ];
							/* current location */
							final double shift = down < i ? rel - reference[ 0 ] : rel + reference[ 0 ];
							shiftsArray[ down ] += shift;
							weightSums[ down ] += w;
						}
					}
				}
			}
		}
	}

}
