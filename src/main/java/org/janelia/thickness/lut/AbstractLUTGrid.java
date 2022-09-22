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
package org.janelia.thickness.lut;

import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public abstract class AbstractLUTGrid implements InvertibleRealTransform
{

	protected final int numSourceDimensions; // number of input dimensions of
												// the transform

	protected final int numTargetDimensions; // number of output dimensions of
												// the transform

	/**
	 * NOT THREADSAFE, AS TO MEMBER 'access' -- THINK ABOUT CHANGING IT!!
	 */
	protected final int lutMaxIndex; // max index of the look-up table

	protected final int nNonTransformedCoordinates; // number of grid dimensions
													// (one less than
													// lutArray.numDimensions())

	protected final Dimensions dimensions; // actual grid dimensions

	protected final RandomAccessibleInterval< DoubleType > lutArray; // look-up
																		// tables

	final protected RealRandomAccessible< RealComposite< DoubleType > > coefficients; // interpolated
																						// composite
																						// of
																						// lutArray

	final protected InterpolatorFactory< RealComposite< DoubleType >, RandomAccessible< RealComposite< DoubleType > > > interpolatorFactory =
			new NLinearInterpolatorFactory< RealComposite< DoubleType > >(); // how
																				// to
																				// interpolate
																				// for
																				// coefficients
//			new NearestNeighborInterpolatorFactory<RealComposite<DoubleType>>();

	protected RealRandomAccess< RealComposite< DoubleType > > access; // temporary
																		// variables

	protected RealComposite< DoubleType > currentLut; // temporary variables

	protected double[] scale;

	protected double[] shift;

	public AbstractLUTGrid( final int numSourceDimensions, final int numTargetDimensions,
			final RandomAccessibleInterval< DoubleType > lutArray )
	{
		this( numSourceDimensions, numTargetDimensions, lutArray, new double[] { 1.0 }, new double[] { 0.0 } );
	}

	public AbstractLUTGrid( final int numSourceDimensions, final int numTargetDimensions,
			final RandomAccessibleInterval< DoubleType > lutArray, final double[] scale, final double[] shift )
	{
		super();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.lutArray = lutArray;

		// generate n-1 dimensional array that has local LUTs as columns
		final CompositeIntervalView< DoubleType, RealComposite< DoubleType > > collapsedSource = Views.collapseReal( lutArray );
		this.dimensions = new FinalInterval( collapsedSource );
		this.nNonTransformedCoordinates = this.dimensions.numDimensions();
		this.lutMaxIndex = ( int ) ( this.lutArray.dimension( this.nNonTransformedCoordinates ) ) - 1;

		// generate scale transform to allow for generating interpolated
		// high-res LUT from low-res LUT
		this.scale = new double[ this.nNonTransformedCoordinates ];
		this.shift = new double[ this.nNonTransformedCoordinates ];
		copyAndFillIfNecessary( scale, this.scale );
		copyAndFillIfNecessary( shift, this.shift );

		final ScaleAndTranslation scaleAndShift = new ScaleAndTranslation( this.scale, this.shift );
		final ExtendedRandomAccessibleInterval< RealComposite< DoubleType >, CompositeIntervalView< DoubleType, RealComposite< DoubleType > > > extendedCollapsedSource =
				Views.extendBorder( collapsedSource );
		this.coefficients = RealViews.transform( Views.interpolate( extendedCollapsedSource, this.interpolatorFactory ), scaleAndShift );
		this.access = this.coefficients.realRandomAccess();
		this.currentLut = this.access.get();

	}

	@Override
	public int numSourceDimensions()
	{
		return this.numSourceDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return this.numTargetDimensions;
	}

	protected double apply( final double lutCoordinate )
	{
		this.currentLut = this.access.get();

		final int zFloor = ( int ) lutCoordinate;

		final double floorVal = this.currentLut.get( zFloor ).get();
		final double nextVal = this.currentLut.get( zFloor + 1 ).get();
		final double dz = lutCoordinate - zFloor;

		return ( nextVal - floorVal ) * dz + floorVal;

	}

	protected double applyChecked( final double lutCoordinate )
	{
		if ( lutCoordinate < 0 )
			return -Double.MAX_VALUE;
		else if ( lutCoordinate > this.lutMaxIndex )
			return Double.MAX_VALUE;
		else if ( lutCoordinate == this.lutMaxIndex )
		{
			return this.access.get().get( this.lutMaxIndex ).get();
		}
		else
			return apply( lutCoordinate );
	}

	/**
	 * 
	 * Implemented as bin-search.
	 *
	 * @return
	 */
	protected int findFloorIndex( final double realLutCoordinate )
	{
//		this.updateCoordinates( gridCoordinates );
		this.currentLut = this.access.get();

		int min = 0;
		int max = this.lutMaxIndex;
		int i = max >> 1;
		do
		{
			if ( currentLut.get( i ).get() > realLutCoordinate )
				max = i;
			else
				min = i;
			// put i in between min and max: i <- 0.5 * (max + min)
			i = ( ( max + min ) >> 1 );
		}
		while ( i != min );
		return i;
	}

	public double applyInverse( final double realLutCoordinate )
	{
		final int i = this.findFloorIndex( realLutCoordinate );

//		this.updateCoordinates( gridCoordinates );
		this.currentLut = this.access.get();

		final double realZ1 = this.currentLut.get( i ).get();
		final double realZ2 = this.currentLut.get( i + 1 ).get();

		return ( realLutCoordinate - realZ1 ) / ( realZ2 - realZ1 ) + i;
	}

	public double applyInverseChecked( final double realLutCoordinate )
	{
//		this.updateCoordinates( gridCoordinates );
		this.currentLut = this.access.get();
		if ( realLutCoordinate < this.currentLut.get( 0 ).get() )
			return -Double.MAX_VALUE;
		if ( realLutCoordinate > this.currentLut.get( this.lutMaxIndex ).get() )
			return Double.MAX_VALUE;
		else
			return this.applyInverse( realLutCoordinate );
	}

	public double minTransformedCoordinate( final double[] gridCoordinates )
	{
		this.updateCoordinates( gridCoordinates );
		return this.access.get().get( 0 ).get();
	}

	public double maxTransformedCoordinate( final double[] gridCoordinates )
	{
		this.updateCoordinates( gridCoordinates );
		return this.access.get().get( this.lutMaxIndex ).get();
	}

	protected void updateCoordinates( final double[] gridCoordinates )
	{
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			this.access.setPosition( gridCoordinates[ d ], d );
		}
	}

	protected void updateCoordinates( final float[] gridCoordinates )
	{
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			this.access.setPosition( gridCoordinates[ d ], d );
		}
	}

	protected void updateCoordinates( final RealLocalizable gridCoordinates )
	{
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d )
		{
			this.access.setPosition( gridCoordinates.getDoublePosition( d ), d );
		}
	}

	protected void copyAndFillIfNecessary( final double[] source, final double[] target )
	{
		final int range = Math.min( source.length, target.length );
		for ( int i = 0; i < range; ++i )
		{
			target[ i ] = source[ i ];
		}
		for ( int i = range; i < target.length; ++i )
		{
			target[ i ] = source[ source.length - 1 ];
		}
	}

}
