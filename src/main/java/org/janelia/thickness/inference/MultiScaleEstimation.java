package org.janelia.thickness.inference;

import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.TranslationModel1D;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.correlations.FloatingPointIntegralCrossCorrelation;
import org.janelia.correlations.ListCorrelationsObject;
import org.janelia.models.ScaleModel;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.mediator.OpinionMediatorModel;

public class MultiScaleEstimation< T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;

	/**
	 * @param images
	 */
	public MultiScaleEstimation(final RandomAccessibleInterval<T> images) {
		super();
		this.images = images;
	}
	
	public RandomAccessibleInterval< DoubleType > estimateZCoordinates(
            final double[] startingCoordinates,
            final int range,
            final long[][] radii,
            final int[][] steps,
            final Visitor visitor,
            final Options[] options) throws NotEnoughDataPointsException, IllDefinedDataPointsException, NotEnoughSpaceException {
		
		assert radii.length == steps.length;
		assert radii.length == options.length;
		
		final TreeMap< Long, List< FloatingPointIntegralCrossCorrelation< T, T, FloatType > > > correlationsMap = new TreeMap<Long, List<FloatingPointIntegralCrossCorrelation<T,T,FloatType>>>();
		final TreeMap< Long, Meta > metaMap = new TreeMap<Long, Meta>();
		
		final long[] r = radii[0];
		
		final long width  = images.dimension( 0 );
		final long height = images.dimension( 1 );
		final long depth  = images.dimension( 2 );
		
		for ( long zRef = 0; zRef < images.dimension( 2 ); ++zRef ) {
			
			final Meta meta = new Meta();
			final long zMin = Math.max( zRef - range, 0 );
			final long zMax = Math.min( zRef + range + 1, images.dimension( 2 ) );
			meta.zPosition = zRef;
			meta.zCoordinateMin = zMin;
			meta.zCoordinateMax = zMax;
			
			final ArrayList<FloatingPointIntegralCrossCorrelation<T, T, FloatType>> al = new ArrayList< FloatingPointIntegralCrossCorrelation< T, T, FloatType > >();
			
			for ( long z = zMin; z < zMax; ++z ) {
				
				final FloatingPointIntegralCrossCorrelation< T, T, FloatType > ccImage;
				if ( z < zRef ) {
					ccImage = correlationsMap.get( z ).get( (int) (zRef - metaMap.get( z ).zCoordinateMin ) );
				} else {
					final IntervalView<T> img1 = Views.hyperSlice( images, 2, zRef );
					final IntervalView<T> img2 = Views.hyperSlice( images, 2, z );
					ccImage = new FloatingPointIntegralCrossCorrelation< T, T, FloatType >( img1, img2, r, new FloatType() );
				}
				al.add( ccImage );

			}
			
			metaMap.put( zRef, meta );
			correlationsMap.put( zRef, al );
			
		}
		
		ArrayImg< DoubleType, DoubleArray > coordinates = ArrayImgs.doubles( width/steps[0][0], height/steps[0][1], depth );
		for ( final ArrayCursor<DoubleType> c = coordinates.cursor(); c.hasNext(); ) {
			c.fwd();
			c.get().set( startingCoordinates[ c.getIntPosition( 2 ) ] );
		}
		
		for ( int i = 0; i < radii.length; ++i ) {
			
			IJ.log( " Radii " + i + ": " + Arrays.toString( radii[i] ) );
			
			for ( final Entry<Long, List<FloatingPointIntegralCrossCorrelation<T, T, FloatType>>> entry : correlationsMap.entrySet() ) {
				for ( final FloatingPointIntegralCrossCorrelation<T, T, FloatType> cc : entry.getValue() )
					cc.setRadius( radii[i] );
			}
			
			final ListCorrelationsObject<T> co = new ListCorrelationsObject<T>( metaMap, correlationsMap );
			
			final ExecutorService es = Executors.newFixedThreadPool( options[i].nThreads );
			
			final int stepX = steps[i][0];
			final int stepY = steps[i][1];
			final long currentWidth  = width / stepX;
			final long currentHeight = height / stepY;
			final Options currentOptions = options[i];
			Scale3D transform;
			if ( i == 0  )
				transform = new Scale3D( 1.0, 1.0, 1.0 );
			else
				transform = new Scale3D( currentWidth * 1.0 / coordinates.dimension( 0 ), currentHeight * 1.0 / coordinates.dimension( 1 ), 1.0 );
			final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> transformed = RealViews.transformReal( Views.interpolate( Views.extendBorder( coordinates ), new NLinearInterpolatorFactory<DoubleType>() ), transform );
			
			
			final ArrayList<double[]> al = new ArrayList<double[]>();
			for ( int k = 0; k < currentWidth * currentHeight; ++k ) {
				al.add( startingCoordinates.clone() );
			}
			final ListImg< double[] > coordinateListImage = new ListImg< double[] >( al, currentWidth, currentHeight );// new ListImgFactory< double[] >().create( new FinalInterval( currentWidth, currentHeight ), new double[ (int) depth ] );
			
			final RealRandomAccess< DoubleType > ra = transformed.realRandomAccess();
			final ArrayList<Callable<Void>> tasks = new ArrayList< Callable<Void> >();
			for ( final Cursor<double[]> c = coordinateListImage.cursor(); c.hasNext(); ) {
				final double[] arr = c.next();
				ra.setPosition( c.getDoublePosition( 0 ), 0 );
				ra.setPosition( c.getDoublePosition( 1 ), 1 );
				for (int j = 0; j < arr.length; j++) {
					ra.setPosition( j, 2 );
					arr[ j ] = ra.get().get();
				}
				final int x = c.getIntPosition( 0 );
				final int y = c.getIntPosition( 1 );
				
				tasks.add( new Callable<Void>() {

					@Override
					public Void call() throws Exception {
						final InferFromCorrelationsObject<TranslationModel1D, ScaleModel> inference = new InferFromCorrelationsObject<TranslationModel1D, ScaleModel>( co, 
								new TranslationModel1D(), 
								new NLinearInterpolatorFactory<DoubleType>(), 
								new ScaleModel(), 
								new OpinionMediatorModel<TranslationModel1D>( new TranslationModel1D() ) );
						inference.estimateZCoordinates( x, y, arr, currentOptions );
						return null;
					}
				});
				
			}
			
			try {
				es.invokeAll(tasks);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			es.shutdown();
//			try {
//				es.awaitTermination( 3600, TimeUnit.SECONDS );
//			} catch (final InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			
			
			
			
			coordinates = ArrayImgs.doubles( currentWidth, currentHeight, depth );
			final RandomAccess<double[]> coordinateRa = coordinateListImage.randomAccess(); 
			for ( final ArrayCursor<DoubleType> c = coordinates.cursor(); c.hasNext(); ) {
				c.fwd();
				coordinateRa.setPosition( c.getIntPosition( 0 ), 0 );
				coordinateRa.setPosition( c.getIntPosition( 1 ), 1 );
				final double[] arr = coordinateRa.get();
				c.get().set( arr[ c.getIntPosition( 2 ) ] );
			}
		}
		
		return coordinates;
	}

}