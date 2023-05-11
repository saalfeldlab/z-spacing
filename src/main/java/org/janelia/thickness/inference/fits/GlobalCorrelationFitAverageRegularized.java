/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2023 Howard Hughes Medical Institute.
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
package org.janelia.thickness.inference.fits;

import net.imglib2.RandomAccessibleInterval;

/**
 *
 * @author Philipp Hanslovsky
 *
 */

public class GlobalCorrelationFitAverageRegularized extends AbstractCorrelationFit
{

	private double[] reg;

	private final double lambda;

	private final GlobalCorrelationFitAverage fit = new GlobalCorrelationFitAverage();

	public GlobalCorrelationFitAverageRegularized( final double[] reg, final double lambda )
	{
		super();
		this.reg = reg;
		this.lambda = lambda;
	}

	@Override
	protected void add( final int z, final int dz, final double value, final double weight )
	{
		fit.add( z, dz, value, weight );
	}

	@Override
	protected void init( final int size )
	{
		fit.init( size );
	}

	@Override
	protected RandomAccessibleInterval< double[] > estimate( final int size )
	{
		final RandomAccessibleInterval< double[] > rai = fit.estimate( size );
		final double oneMinusLambda = 1.0 - lambda;
		final double[] current = rai.randomAccess().get();
		for ( int i = 0; i < Math.min( current.length, this.reg.length ); ++i )
		{
			final double v1 = current[ i ];
			final double v2 = reg[ i ];
			current[ i ] = Double.isNaN( v1 ) ? v2 : Double.isNaN( v2 ) ? v1 : oneMinusLambda * v1 + lambda * v2;
		}
		// TODO Do this or not?
		this.reg = current;
		return rai;
	}

}
