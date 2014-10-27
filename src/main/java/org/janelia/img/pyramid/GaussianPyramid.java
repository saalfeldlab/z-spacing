package org.janelia.img.pyramid;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class GaussianPyramid< T extends RealType< T > > extends AbstractPyramid< T > {
	
	public GaussianPyramid( final RandomAccessibleInterval< T > source, 
			final double scalingFactor,
			final double sigma, 
			final ImgFactory< T > factory ) {
		super( sourceToListOfScales(source, scalingFactor <= 1.0 ? scalingFactor : 1.0/scalingFactor, sigma, factory, source.randomAccess().get() ),
				factory );
	}
	
	public static <U extends RealType< U > > List< RandomAccessibleInterval< U > > sourceToListOfScales( final RandomAccessibleInterval< U > source, 
			final double scalingFactor, 
			final double sigma, 
			final ImgFactory< U > factory, 
			final U type ) {
		
		final RandomAccessibleInterval<U> source3D;
		if ( source.numDimensions() == 2 ) {
			source3D = Views.addDimension( source, 0, 0 );
		} else {
			source3D = source;
		}
		
		assert( source3D.numDimensions() == 3 );
		
		
		final ArrayList<RandomAccessibleInterval<U>> images = new ArrayList< RandomAccessibleInterval< U > >();
		images.add( source );
		
		RandomAccessibleInterval<U> previous = source3D;
		long shortDimension = Math.min( previous.dimension( 0 ), previous.dimension( 1 ) );
		
		while ( shortDimension > 1 ) {
			
			final FinalInterval dim = new FinalInterval( (long) ( previous.dimension( 0 )*scalingFactor ), (long)( previous.dimension( 1 )*scalingFactor ), source3D.dimension( 2 ) );
			final RandomAccessibleInterval<U> scaled  = factory.create( source3D, type );
			final RandomAccessibleInterval<U> scaled2 = Views.interval( Views.raster( RealViews.transformReal( Views.interpolate( Views.extendBorder( scaled ), new NLinearInterpolatorFactory< U >()), new Scale( scalingFactor, scalingFactor, 1 ) ) ), dim ); 
			final RandomAccessibleInterval<U> result  = factory.create( dim, type );
			
			for ( int i = 0; i < scaled.dimension( 2 ); ++i ) {
				try {
					Gauss3.gauss( sigma, Views.extendMirrorSingle( Views.hyperSlice( previous, 2, i ) ), Views.hyperSlice( scaled, 2, i ) );
				} catch (final IncompatibleTypeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			final Cursor<U> r = Views.flatIterable( result ).cursor();
			final Cursor<U> s = Views.flatIterable( scaled2 ).cursor();
					
			while ( r.hasNext() )
				r.next().set( s.next() );
			
			images.add( result );
			
			previous = result;
			shortDimension = Math.min( result.dimension( 0 ), result.dimension( 1 ) );
		}
		
		return images;
	}
	
	

}
