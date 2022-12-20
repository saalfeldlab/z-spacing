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
package org.janelia.thickness.inference;

import java.util.Arrays;

import org.janelia.thickness.inference.InferFromMatrix.RegularizationType;
import org.janelia.thickness.inference.fits.GlobalCorrelationFitAverage;
import org.junit.Assert;
import org.junit.Test;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

public class InferFromMatrixTest
{

	@Test
	public void testNoScalingFactors() throws Exception
	{

		final int size = 20;

		final int range = 5;

		final double maxSimilarity = 1.0;

		final double minSimilarity = 0.5;

		final ArrayImg< DoubleType, DoubleArray > matrix = ArrayImgs.doubles( size, size );

		final int rupture = size / 2;

		final double diminishingFactor = 0.9; // > 0, < 1

		final ArrayCursor< DoubleType > c = matrix.cursor();
		final double step = ( maxSimilarity - minSimilarity ) / range;
		while ( c.hasNext() )
		{
			c.fwd();
			final long x = c.getLongPosition( 0 );
			final long y = c.getLongPosition( 1 );
			final long dx = Math.abs( x - y );
			final double diminish = ( x < rupture && y >= rupture ) || ( y < rupture && x >= rupture ) ? diminishingFactor * step : 0;
			final double sim = dx <= range ? maxSimilarity - dx * step - diminish : Double.NaN;
			c.get().set( sim );
		}

		final double[] startingCoordinates = new double[ size ];
		final double[] scalingFactors = new double[ size ];
		for ( int z = 0; z < size; ++z )
		{
			startingCoordinates[ z ] = z;
			scalingFactors[ z ] = 1.0;
		}

		final Options o = Options.generateDefaultOptions();
		o.comparisonRange = range;
		o.withReorder = false;
		o.nIterations = 100;
		o.regularizationType = RegularizationType.NONE;
		o.scalingFactorEstimationIterations = 0;
		o.scalingFactorRegularizerWeight = 1.0;
		o.shiftProportion = 1.0;

		final InferFromMatrix inf = new InferFromMatrix( new GlobalCorrelationFitAverage() );
		final double[] coordinates = inf.estimateZCoordinates( matrix, startingCoordinates, o );

		final double unitDist = coordinates[ 1 ] - coordinates[ 0 ];
		System.out.println( Arrays.toString( coordinates ) );
		for ( int z = 0; z < coordinates.length - 1; ++z )
			Assert.assertEquals( z == rupture - 1 ? 1.0 + diminishingFactor : 1.0, ( coordinates[ z + 1 ] - coordinates[ z ] ) / unitDist, 1e-10 );
	}

}
