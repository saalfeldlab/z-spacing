package org.janelia.thickness.inference;

import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.imglib2.img.list.ListRandomAccess;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.NotEnoughSpaceException;
import org.janelia.correlations.storage.DenseCorrelationMatricesWithRadius;
import org.janelia.thickness.cluster.Categorizer;
import org.janelia.thickness.cluster.RangedCategorizer;
import org.janelia.thickness.inference.visitor.LazyVisitor;
import org.janelia.thickness.inference.visitor.multiscale.LazyMultiScaleVisitor;
import org.janelia.thickness.inference.visitor.multiscale.MultiScaleVisitor;
import org.janelia.thickness.mediator.OpinionMediatorModel;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T>
 */
public class MultiScaleEstimation {
	
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< DoubleType > estimateZCoordinates(
			final DenseCorrelationMatricesWithRadius< T > matrices,
            final double[] startingCoordinates,
            final int range,
            final long[][] radii,
            final int[][] steps,
            final Options[] options) throws NotEnoughDataPointsException, IllDefinedDataPointsException, NotEnoughSpaceException {
		return estimateZCoordinates( matrices, startingCoordinates, range, radii, steps, new LazyMultiScaleVisitor(), options );
	}
	
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< DoubleType > estimateZCoordinates(
			final DenseCorrelationMatricesWithRadius< T > matrices,
            final double[] startingCoordinates,
            final int range,
            final long[][] radii,
            final int[][] steps,
            final MultiScaleVisitor visitor,
            final Options[] options) throws NotEnoughDataPointsException, IllDefinedDataPointsException, NotEnoughSpaceException {
		final RangedCategorizer categorizer = new RangedCategorizer( startingCoordinates.length );
		categorizer.generateLabels( startingCoordinates.length );
		return estimateZCoordinates( matrices, startingCoordinates, range, radii, steps, visitor, categorizer, options );
	}
	
	
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< DoubleType > estimateZCoordinates(
			final DenseCorrelationMatricesWithRadius< T > matrices,
            final double[] startingCoordinates,
            final int range,
            final long[][] radii,
            final int[][] steps,
            final MultiScaleVisitor visitor,
            final Categorizer categorizer,
            final Options[] options) throws NotEnoughDataPointsException, IllDefinedDataPointsException, NotEnoughSpaceException {
		
		assert radii.length == steps.length;
		assert radii.length == options.length;
		
		final long width  = matrices.dimension( 0 );
		final long height = matrices.dimension( 1 );
		final long depth  = matrices.randomAccess().get().dimension( 0 );
		
		
		ArrayImg< DoubleType, DoubleArray > coordinates = ArrayImgs.doubles( width/steps[0][0], height/steps[0][1], depth );
		for ( final ArrayCursor<DoubleType> c = coordinates.cursor(); c.hasNext(); ) {
			c.fwd();
			c.get().set( startingCoordinates[ c.getIntPosition( 2 ) ] );
		}
		
		for ( int i = 0; i < radii.length; ++i ) {
			
			matrices.setRadius( radii[i] );
			
			categorizer.setState( i );
			
			final ExecutorService es = Executors.newFixedThreadPool( 1 ); // do not use es here?
			
			final int stepX = steps[i][0];
			final int stepY = steps[i][1];
			final long currentWidth  = width / stepX;
			final long currentHeight = height / stepY;
			final Options currentOptions = options[i];
			final Scale3D transform = new Scale3D( currentWidth * 1.0 / coordinates.dimension( 0 ), currentHeight * 1.0 / coordinates.dimension( 1 ), 1.0 );
			final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> transformed = RealViews.transformReal( Views.interpolate( Views.extendBorder( coordinates ), new NLinearInterpolatorFactory<DoubleType>() ), transform );
			
			
			final ArrayList<double[]> al = new ArrayList<double[]>();
			for ( int k = 0; k < currentWidth * currentHeight; ++k ) {
				al.add( startingCoordinates.clone() );
			}
			IJ.log( "WxH = " + currentWidth + "x" + currentHeight );
			final ListImg< double[] > coordinateListImage = new ListImg< double[] >( al, currentWidth, currentHeight );// new ListImgFactory< double[] >().create( new FinalInterval( currentWidth, currentHeight ), new double[ (int) depth ] );
			
			final RealRandomAccess< DoubleType > ra                    = transformed.realRandomAccess();
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
				
				final RandomAccess<RandomAccessibleInterval< T > > m = matrices.randomAccess();
				m.setPosition( x, 0 );
				m.setPosition( y, 1 );
				
				final RandomAccessibleInterval<T> currMat = m.get();
				
				tasks.add( new Callable<Void>() {

					@Override
					public Void call() throws Exception {
						final InferFromMatrix<TranslationModel1D> inference = new InferFromMatrix<TranslationModel1D>( 
								new TranslationModel1D(), 
								new OpinionMediatorModel<TranslationModel1D>( new TranslationModel1D() ) );
						
						final double[] lut = inference.estimateZCoordinates( currMat, arr, new LazyVisitor(), categorizer, currentOptions );
						// IJ.log( "Iterating at c=" + x + "x" + y + " " + Arrays.toString( lut ) );
						final ListRandomAccess<double[]> rax = coordinateListImage.randomAccess();
						rax.setPosition( new int[] { x, y } );
						rax.set( lut );
						return null;
					}
				});
				
			}
			
			
			{
				IJ.log( "before inference" );
				final long t0 = System.currentTimeMillis();
				try {
					es.invokeAll(tasks);
				} catch (final InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				final long t1 = System.currentTimeMillis();
				final long diff = t1 - t0;
				IJ.log( "after inference (t=" + diff + "ms)" );
			}

			
			coordinates = ArrayImgs.doubles( currentWidth, currentHeight, depth );
			final RandomAccess<double[]> coordinateRa = coordinateListImage.randomAccess(); 
			for ( final ArrayCursor<DoubleType> c = coordinates.cursor(); c.hasNext(); ) {
				c.fwd();
				coordinateRa.setPosition( c.getIntPosition( 0 ), 0 );
				coordinateRa.setPosition( c.getIntPosition( 1 ), 1 );
				final double[] arr = coordinateRa.get();
				c.get().set( arr[ c.getIntPosition( 2 ) ] );
			}

			IJ.log( "before rendering" );
			final long t0 = System.currentTimeMillis();
			visitor.act( i, coordinates, Views.interval( Views.raster( transformed ), coordinates), radii[i], steps[i], options[i] );
			final long t1 = System.currentTimeMillis();
			final long diff = t1 - t0;
			IJ.log( "after rendering (t=" + diff + "ms)" );
			
		}
		
		return coordinates;
	}

}
