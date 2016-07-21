package org.janelia.thickness.inference.fits;

import java.util.ArrayList;

import org.janelia.thickness.inference.Options;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

public class LocalCorrelationFitAverage extends AbstractCorrelationFit
{
	int estimateWindowRadius;

	ScaleAndTranslation scaleAndTranslation;

	ArrayImg< DoubleType, DoubleArray > summedMeasurements;

	ArrayRandomAccess< DoubleType > measurementsAccess;

	int[][] nSamples;

	double[] transformCoordinate;


	public LocalCorrelationFitAverage( int dimension, Options o )
	{
		estimateWindowRadius = o.estimateWindowRadius < 1 ? dimension : Math.min( o.estimateWindowRadius, dimension );
		int nFits = Math.max( dimension / estimateWindowRadius, 1 );
		double[] translation = new double[] { estimateWindowRadius };
		double[] scale = new double[] { estimateWindowRadius };
		scaleAndTranslation = new ScaleAndTranslation( scale, translation );
		summedMeasurements = ArrayImgs.doubles( nFits, o.comparisonRange + 1 );
		measurementsAccess = summedMeasurements.randomAccess();
		nSamples = new int[ nFits ][ o.comparisonRange + 1 ];
		transformCoordinate = new double[ 1 ];
	}

	@Override
	protected void add( int z, int dz, double value )
	{
		transformCoordinate[ 0 ] = z;
		scaleAndTranslation.applyInverse( transformCoordinate, transformCoordinate );
		if ( transformCoordinate[0] <= 0.0 )
			addLocal( 0, dz, value );
		else if ( transformCoordinate[ 0 ] >= nSamples.length - 1 )
			addLocal( nSamples.length - 1, dz, value );
		else
		{
			int lower = ( int ) Math.floor( transformCoordinate[ 0 ] );
			addLocal( lower, dz, value );
			addLocal( lower + 1, dz, value );
		}

	}

	private void addLocal( int localZ, int dz, double value )
	{
		measurementsAccess.setPosition( localZ, 0 );
		measurementsAccess.setPosition( dz, 1 );
		measurementsAccess.get().add( new DoubleType( value ) );
		nSamples[ localZ ][ dz ] += 1;
	}

	@Override
	protected void init( int size )
	{
		ArrayCursor< DoubleType > c = summedMeasurements.cursor();
		int sizePlusOne = size + 1;
		for ( int n = 0; n < nSamples.length; ++n )
		{
			int[] ns = nSamples[n];
			for ( int r = 0; r < sizePlusOne; ++r )
			{
				ns[ r ] = 0;
				c.next().set( 0.0 );
			}
		}
	}

	@Override
	protected RandomAccessibleInterval< double[] > estimate( int size )
	{

		{
			ArrayRandomAccess< DoubleType > ra = summedMeasurements.randomAccess();
			for ( int n = 0; n < nSamples.length; ++n )
			{
				ra.setPosition( n, 0 );
				int[] ns = nSamples[ n ];
				for ( int r = 0; r < ns.length; ++r )
				{
					ra.setPosition( r, 1 );
					ra.get().mul( 1.0 / ns[ r ] );
				}
			}
		}


		CompositeIntervalView< DoubleType, RealComposite< DoubleType > > collapsed =
				Views.collapseReal( summedMeasurements );

		RealRandomAccessible< RealComposite< DoubleType > > interpolated =
				Views.interpolate( Views.extendBorder( collapsed ), new NLinearInterpolatorFactory<>() );

		FinalInterval fi = new FinalInterval( size );
		IntervalView< RealComposite< DoubleType > > transformed =
				Views.interval( Views.raster( RealViews.transformReal( interpolated, scaleAndTranslation ) ), fi );

		Cursor< RealComposite< DoubleType > > t = transformed.cursor();
		ArrayList< double[] > list = new ArrayList<>();
		for ( int n = 0; n < size; ++n )
		{
			RealComposite< DoubleType > c = t.next();
			double[] target = new double[ nSamples[ 0 ].length ];
			target[ 0 ] = -1;
			for ( int i = 1; i < target.length; ++i )
				target[ i ] = -c.get( i ).get();
			list.add( target );
		}

		return new ListImg<>( list, list.size() );
	}

}
