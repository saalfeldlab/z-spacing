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

import java.util.Arrays;

import org.janelia.thickness.inference.Options;
import org.junit.Assert;
import org.junit.Test;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;

public class ShiftCoordinatesTest
{



	@Test
	public void testNoShifts()
	{

		final int size = 20;

		final int range = 5;

		final double maxSimilarity = 1.0;

		final double minSimilarity = 0.5;

		final ArrayImg< DoubleType, DoubleArray > matrix = ArrayImgs.doubles( size, size );

		final double[] fit = new double[ range + 1 ];

		final RandomAccessibleInterval< double[] > fits = ConstantUtils.constantRandomAccessibleInterval( fit, 1, new FinalInterval( size ) );

		final long maxIndex = size - 1;
		final ArrayCursor< DoubleType > c = matrix.cursor();
		final double step = ( maxSimilarity - minSimilarity ) / range;
		while( c.hasNext() )
		{
			c.fwd();
			final long x = c.getLongPosition( 0 );
			final long y = c.getLongPosition( 1 );
			final long dx = Math.abs( x - y );
			double sim = dx <= range ? maxSimilarity - dx * step : Double.NaN;
			if ( x == maxIndex || y == maxIndex )
				sim /= 2.0;
			c.get().set( x == y ? 1.0 : sim );
		}

		for ( int dz = 0; dz < fit.length; ++dz )
			fit[ dz ] = -( maxSimilarity - dz * step );

		final double[] coordinates = new double[ size ];
		final double[] scalingFactors = new double[ size ];
		for ( int z = 0; z < size; ++z )
		{
			coordinates[ z ] = z;
			scalingFactors[ z ] = 1.0;
		}
		scalingFactors[ size - 1 ] = 2.0;


		final Options o = Options.generateDefaultOptions();
		o.comparisonRange = range;

		final double[] shiftsArray = new double[ size ];
		final double[] weightSums = new double[ size ];
		final double[] shiftWeights = Arrays.stream( new double[ size ] ).map( d -> 1.0 ).toArray();

		ShiftCoordinates.collectShiftsFromMatrix(
				coordinates,
				scaleMatrix( matrix, scalingFactors ),
				scalingFactors,
				fits,
				shiftsArray,
				weightSums,
				shiftWeights,
				o );
		for ( int z = 0; z < size; ++z )
		{
			final long missing = z > range ? z < size - range ? 0 : range - ( size - 1 - z ) : range - z;
			final long expectedNumberOfVotes = 2 * range - missing;

			Assert.assertEquals( expectedNumberOfVotes, ( int ) weightSums[ z ] );
			Assert.assertEquals( 0.0, shiftsArray[ z ], 0.0 );
		}
	}

	private static Img< DoubleType > scaleMatrix( final Img< DoubleType > matrix, final double[] scalingFactors )
	{
		final long[] dim = new long[ matrix.numDimensions() ];
		matrix.dimensions( dim );
		final Img< DoubleType > scaledMatrix = matrix.factory().create( dim, matrix.firstElement().createVariable() );
		for ( Cursor< DoubleType > m = matrix.cursor(), s = scaledMatrix.cursor(); s.hasNext(); )
		{
			m.fwd();
			s.fwd();
			s.get().set( scalingFactors[ m.getIntPosition( 0 ) ] * scalingFactors[ m.getIntPosition( 1 ) ] * m.get().get() );
		}
		return scaledMatrix;
	}

}
