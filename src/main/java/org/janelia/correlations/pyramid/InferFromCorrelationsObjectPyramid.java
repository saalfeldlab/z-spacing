package org.janelia.correlations.pyramid;

import ij.IJ;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.TranslationModel1D;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.models.ScaleModel;
import org.janelia.thickness.inference.InferFromCorrelationsObject;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.mediator.OpinionMediatorModel;

public class InferFromCorrelationsObjectPyramid {
	

	private final CorrelationsObjectPyramid pyramid;

	/**
	 * @param pyramid
	 */
	public InferFromCorrelationsObjectPyramid(final CorrelationsObjectPyramid pyramid) {
		super();
		this.pyramid = pyramid;
	}
	
	public RandomAccessibleInterval< DoubleType > estimateZCoordinates(
            final double[] startingCoordinates,
            final Visitor visitor,
            final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		Img<double[]> listResult = new ListImgFactory< double[] >().create( new FinalInterval( 1, 1 ), startingCoordinates );
		
		final double[] zeros = new double[ startingCoordinates.length ];
		
		for ( int level = 0; level < pyramid.getNumberOfLevels(); ++level ) {
			IJ.log( "Inferring at level " + level );
			final CorrelationsObjectInterface co = pyramid.get( level );
			final ExecutorService es = Executors.newFixedThreadPool( options.nThreads );
			final Img<double[]> previous = listResult;
			
			final long xMin = co.getxMin();
			final long xMax = co.getxMax();
			final long yMin = co.getyMin();
			final long yMax = co.getyMax();
			
			final long xRange = xMax - xMin;
			final long yRange = yMax - yMin;
			
			listResult = new ListImgFactory< double[] >().create( new FinalInterval( xRange, yRange ), zeros.clone() );
			
			final double[] scales        = new double[] { listResult.dimension( 0 )*1.0/previous.dimension( 0 ), listResult.dimension( 1 )*1.0/previous.dimension( 1 ) };
			final double[] inverseScales = new double[] { 1.0/scales[0], 1.0/scales[1] };
			
			final RandomAccess<double[]> ra     = previous.randomAccess();
			final RandomAccess<double[]> target = listResult.randomAccess();
			final double[] weights = new double[ 4 ];
			final long[] neighborIndices = new long[ 4 ];
			final long[][] positions = new long[ 4 ][ 2 ];
			final double[] orig = new double[ 2 ];
			
			for ( int y = 0; y < yRange; ++y ) {
				orig[1] = y * inverseScales[1];
				neighborIndices[2] = (long) Math.floor( orig[1] );
				neighborIndices[3] = (long) Math.ceil( orig[1] );
				target.setPosition( y, 1);
				for ( int x = 0; x < xRange; ++x ) {
					target.setPosition( x, 0 );
					final double[] targetCoord = target.get();
					orig[0] = x * inverseScales[0];
					neighborIndices[0] = (long) Math.floor( orig[0] );
					neighborIndices[1] = (long) Math.ceil( orig[0] );
					
					positions[ 0 ][ 0 ] = neighborIndices[ 0 ];
					positions[ 0 ][ 1 ] = neighborIndices[ 2 ];
					positions[ 1 ][ 0 ] = neighborIndices[ 0 ];
					positions[ 1 ][ 1 ] = neighborIndices[ 3 ];
					positions[ 2 ][ 0 ] = neighborIndices[ 1 ];
					positions[ 2 ][ 1 ] = neighborIndices[ 2 ];
					positions[ 3 ][ 0 ] = neighborIndices[ 1 ];
					positions[ 3 ][ 1 ] = neighborIndices[ 3 ];
					
					weights[0] = Math.abs( ( neighborIndices[0] - orig[0] )*( neighborIndices[2] - orig[1] ) );
					weights[1] = Math.abs( ( neighborIndices[0] - orig[0] )*( neighborIndices[3] - orig[1] ) );
					weights[2] = Math.abs( ( neighborIndices[1] - orig[0] )*( neighborIndices[2] - orig[1] ) );
					weights[3] = Math.abs( ( neighborIndices[1] - orig[0] )*( neighborIndices[3] - orig[1] ) );
					
					for ( int i = 0; i < positions.length; ++i ) {
						ra.setPosition( positions[ i ] );
						final double w = weights[i];
						final double[] coord = ra.get();
						for ( int z = 0; z < coord.length; ++z ) {
							targetCoord[z] += w*coord[z]; 
						}
					}
				}
			}
			
			for ( final Cursor< double[] > c = listResult.cursor(); c.hasNext(); ) {
				final double[] coord = c.next();
				es.submit( new Callable< Void >() {

					@Override
					public Void call() throws Exception {
						final InferFromCorrelationsObject<TranslationModel1D, ScaleModel> inference = new InferFromCorrelationsObject< TranslationModel1D, ScaleModel>( co, new TranslationModel1D(), new NLinearInterpolatorFactory<DoubleType>(), new ScaleModel(), new OpinionMediatorModel< TranslationModel1D>(new TranslationModel1D()));
						final ArrayImg<DoubleType, DoubleArray> ret = inference.estimateZCoordinates( c.getIntPosition(0), c.getIntPosition(1), coord, visitor, options );
						for ( final ArrayCursor< DoubleType > rc = ret.cursor(); rc.hasNext(); ) {
							rc.fwd();
							coord[ rc.getIntPosition( 0 ) ] = rc.get().get();
						}
							
						return null;
					}
				});
			}
			
		}
		
		final ArrayImg< DoubleType, DoubleArray > result = ArrayImgs.doubles( listResult.dimension( 0 ), listResult.dimension( 1 ), listResult.firstElement().length );
		
		for ( final Cursor< double[] > c = listResult.cursor(); c.hasNext(); ) {
			final double[] coord = c.next();
			final Cursor<DoubleType> hs = Views.flatIterable( Views.hyperSlice( Views.hyperSlice( result, 1, c.getIntPosition( 1 ) ), 0, c.getIntPosition( 0 ) ) ).cursor();
			for ( int z = 0; hs.hasNext(); ++z ) {
				hs.next().set( coord[z] );
			}
		}
		
		return result;
	}
	
	
}
