package org.janelia.img.pyramid;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class AbstractPyramid< T extends RealType< T > > implements PyramidInterface< T > {
	
	private final List< RandomAccessibleInterval< T > > images;
	private final int levels;
	private final int maxLevel;
	private final ImgFactory< T > factory;
	
	public AbstractPyramid( final List< RandomAccessibleInterval< T > > images, final ImgFactory< T > factory ) {
		
		this.images   = images;
		this.factory  = factory;
		this.levels   = this.images.size();
		this.maxLevel = this.levels - 1;
		
	}
	
	
	/**
	 * @return the maxLevel (equivalent to maximum smoothing/smallest image in the pyramid)
	 */
	public int getMaxLevel() {
		return maxLevel;
	}


	@Override
	public RandomAccessibleInterval< T > get( final int level ) {
		return this.images.get( level );
	}
	
	@Override
	public RandomAccessibleInterval< T > get( final double level ) {
		
		final int level0 = (int) Math.floor( level );
		final int level1 = (int) Math.ceil( level );
		
		if ( level0 == level1 )
			return this.images.get( level0 );
		
		final ArrayList< RandomAccessibleInterval< T > > tmpList= new ArrayList< RandomAccessibleInterval< T > >();
		
		final RandomAccessibleInterval<T> lower = this.images.get( level0 );
		final RandomAccessibleInterval<T> upper = this.images.get( level1 );
		
		final double[] scalingFactors = new double[ lower.numDimensions() ];
		for (int i = 0; i < scalingFactors.length; i++) {
			scalingFactors[i] = lower.dimension( i )*1.0/upper.dimension( i );
		}
//		{ lower.dimension( 0 )*1.0/upper.dimension( 0 ), lower.dimension( 1 )*1.0/upper.dimension( 1 ) };
		
		tmpList.add( lower );
		tmpList.add( Views.interval(
				Views.raster( 
						RealViews.transform( 
								Views.interpolate( Views.extendBorder( upper ), new NLinearInterpolatorFactory< T >()), 
								new Scale2D( scalingFactors ) )
							), 
				lower ) );
		
		// creating an interpolated view might be nicer than allocating a new image
		final Img<T> result = this.factory.create( lower, lower.randomAccess().get() );
		
		final Cursor<T> lC = Views.flatIterable( tmpList.get( 0 ) ).cursor();
		final Cursor<T> uC = Views.flatIterable( tmpList.get( 1 ) ).cursor();
		final Cursor<T> rC = result.cursor();
		
		final double lowerWeight = level1 - level;
		final double upperWeight = level - level0;
		
		while( lC.hasNext() ) {
			final double val = lC.next().getRealDouble()*lowerWeight + uC.next().getRealDouble()*upperWeight;
			rC.next().setReal( val );
		}
	

		return result;
	}
	

}
