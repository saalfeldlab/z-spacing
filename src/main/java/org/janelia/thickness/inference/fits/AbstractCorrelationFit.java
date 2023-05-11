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

import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.AbstractLUTRealTransform;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky
 */
public abstract class AbstractCorrelationFit
{

	public < T extends RealType< T >, W extends RealType< W > > RandomAccessibleInterval< double[] > estimateFromMatrix(
			final RandomAccessibleInterval< T > correlations,
			final double[] coordinates,
			final AbstractLUTRealTransform transform,
			final RandomAccessibleInterval< W > estimateWeightMatrix,
			final Options options,
			InterpolatorFactory< T, RandomAccessible< T > > interpolatorFactory )
	{
		final int range = options.comparisonRange;
		final boolean forceMonotonicity = options.forceMonotonicity;

		final T correlationsNaNExtension = correlations.randomAccess().get().copy();
		correlationsNaNExtension.setReal( Double.NaN );
		final RealRandomAccessible< T > extendedInterpolatedCorrelations = Views.interpolate( Views.extendValue( correlations, correlationsNaNExtension ), interpolatorFactory );

		final RealTransformRealRandomAccessible< T, InverseRealTransform > transformedCorrelations = RealViews.transformReal( extendedInterpolatedCorrelations, transform );

		// TODO extend border or value (nan)?
		final RealRandomAccessible< W > extendedInterpolatedWeights = Views.interpolate( Views.extendBorder( estimateWeightMatrix ), new NLinearInterpolatorFactory<>() );

		final RealTransformRealRandomAccessible< W, InverseRealTransform > transformedWeights = RealViews.transformReal( extendedInterpolatedWeights, transform );

		final RealRandomAccess< T > access1 = transformedCorrelations.realRandomAccess();
		final RealRandomAccess< T > access2 = transformedCorrelations.realRandomAccess();

		final RealRandomAccess< W > wAccess1 = transformedWeights.realRandomAccess();
		final RealRandomAccess< W > wAccess2 = transformedWeights.realRandomAccess();

		init( range );

		for ( int z = 0; z < coordinates.length; ++z )
		{
			access1.setPosition( z, 1 );
			access1.setPosition( z, 0 );
			transform.apply( access1, access1 );
			access2.setPosition( access1 );

			wAccess1.setPosition( access1 );
			wAccess2.setPosition( access1 );

			double currentMin1 = Double.MAX_VALUE;
			double currentMin2 = Double.MAX_VALUE;
			// should w go in pairwise?
			for ( int k = 0; k <= range; ++k, access1.fwd( 0 ), access2.bck( 0 ), wAccess1.fwd( 0 ), wAccess2.bck( 0 ) )
			{
				final double a1 = access1.get().getRealDouble();
				final double a2 = access2.get().getRealDouble();
				if ( !Double.isNaN( a1 ) && a1 > 0.0 && ( !forceMonotonicity || a1 < currentMin1 ) )
				{
					currentMin1 = a1;
					add( z, k, a1, wAccess1.get().getRealDouble() );
				}
				if ( !Double.isNaN( a2 ) && a2 > 0.0 && ( !forceMonotonicity || a2 < currentMin2 ) )
				{
					currentMin2 = a2;
					add( z, k, a2, wAccess2.get().getRealDouble() );
				}
			}
		}

		return estimate( coordinates.length );
	}


	protected abstract void add( int z, int dz, double value, double weight );

	protected abstract void init( int size );

	// TODO change return type to RandomAccessibleInterval< RealComposite< DoubleType > >
	protected abstract RandomAccessibleInterval< double[] > estimate( int size );

}
