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
import org.junit.Assert;
import org.junit.Test;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;

public class EstimateScalingFactorsTest
{

	double[] matrixArray = {
			1.00, 0.90, 0.80, 0.35,
			0.90, 1.00, 0.90, 0.40,
			0.80, 0.90, 1.00, 0.45,
			0.35, 0.40, 0.45, 1.00
	};

	ArrayImg< DoubleType, DoubleArray > matrix = ArrayImgs.doubles( matrixArray, 4, 4 );

	double[] fit = { -1.00, -0.90, -0.80, -0.70 };

	RandomAccessibleInterval< double[] > localFits = ConstantUtils.constantRandomAccessibleInterval( fit, 1, new FinalInterval( matrix.dimension( 0 ) ) );

	final int comparisonRange = fit.length - 1;

	@Test
	public void testFullRegularization()
	{
		for ( int i = 1; i <= 10; ++i )
		{
			final double[] scalingFactors = runScalingFactorsEstimation( 1.0, i );
			final double[] expected = new double[ scalingFactors.length ];
			for ( int z = 0; z < expected.length; ++z )
				expected[ z ] = 1.0;
			Assert.assertArrayEquals( expected, scalingFactors, 0.0 );
		}
	}

	@Test
	public void testNoRegularization()
	{
		final int nIterations = 50;
		final double[] scalingFactors = runScalingFactorsEstimation( 0.0, nIterations );
		final double[] expected = new double[ scalingFactors.length ];
		for ( int z = 0; z < expected.length; ++z )
			expected[ z ] = 1.0;
		expected[ expected.length - 1 ] = 2.0;
		Assert.assertArrayEquals( expected, scalingFactors, 0.0 );
	}

	@Test
	public void testDefaultRegularization()
	{

		final double regularization = Options.generateDefaultOptions().scalingFactorRegularizerWeight;

		final int nIterations = 50;
		final double[] scalingFactors = runScalingFactorsEstimation( regularization, nIterations );
		final double[] expected = new double[ scalingFactors.length ];
		for ( int z = 0; z < expected.length; ++z )
			expected[ z ] = 1.0;
		expected[ expected.length - 1 ] = 2.0;
		for ( int z = 0; z < expected.length; ++z )
			Assert.assertEquals( expected[ z ], scalingFactors[ z ], regularization * expected[ z ] );
	}

	public double[] runScalingFactorsEstimation( final double regularizerWeight, final int nIterations )
	{

		final double[] scalingFactors = new double[ ( int ) matrix.dimension( 0 ) ];
		final double[] coordinates = new double[ scalingFactors.length ];

		for ( int z = 0; z < coordinates.length; ++z )
		{
			scalingFactors[ z ] = 1.0;
			coordinates[ z ] = z;
		}
		EstimateScalingFactors.estimateQuadraticFromMatrix( matrix, scalingFactors, coordinates, localFits, regularizerWeight, comparisonRange, nIterations, ConstantUtils.constantRandomAccessibleInterval( new DoubleType( 1.0 ), 2, matrix ) );
		return scalingFactors;
	}

}
