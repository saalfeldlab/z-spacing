package org.janelia.thickness;

import java.util.Iterator;

import mpicbg.models.Model;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.visitor.LocalVisitor;
import org.janelia.thickness.lut.LUTGrid;
import org.janelia.thickness.mediator.OpinionMediator;


public class EstimateThicknessLocally< M extends Model<M>, L extends Model<L> > {
	
	private final M correlationFitModel;
	private final L measurementsMultiplierModel;
	private final OpinionMediator shiftMediator;
	private final CorrelationsObjectInterface correlationsObject;
	private final long zMin;
    private final long zMax;
	
	
	


	public EstimateThicknessLocally(final M correlationFitModel,
			final L measurementsMultiplierModel, final OpinionMediator shiftMediator,
			final CorrelationsObjectInterface correlations) {
		super();
		this.correlationFitModel = correlationFitModel;
		this.measurementsMultiplierModel = measurementsMultiplierModel;
		this.shiftMediator = shiftMediator;
		this.correlationsObject = correlations;
		final Iterator<Long> iterator = this.correlationsObject.getMetaMap().keySet().iterator();
        zMin = iterator.next();
        long zMaxTmp = zMin;

        while ( iterator.hasNext() )
                zMaxTmp = iterator.next();
        zMax = zMaxTmp + 1;
	}



	public static class Options {

        public static Options generateDefaultOptions() {
            final Options result = new Options();
            result.multiplierGenerationRegularizerWeight = 0.01;
            result.coordinateUpdateRegularizerWeight = 0.01;
            result.shiftProportion = 0.5;
            result.nIterations = 1;
            result.nThreads = 1;
            result.comparisonRange = 10;
            result.neighborRegularizerWeight = 0.05;
            return result;
        }

        public double multiplierGenerationRegularizerWeight; // m_regularized = m * ( 1 - w ) + 1 * w
        public double coordinateUpdateRegularizerWeight; // coordinate_regularized = predicted * ( 1 - w ) + original * w
        public double shiftProportion; // actual_shift = shift * shiftProportion
        public int nIterations; // number of iterations
        public int nThreads; // number of threads
        public int comparisonRange; // range for cross correlations
        public double neighborRegularizerWeight;


	}
	
	
	public ArrayImg< DoubleType, DoubleArray > estimate(
			final int startX,
			final int startY,
			final int stopX,
			final int stopY,
			final int stepX,
			final int stepY,
			final LocalVisitor visitor,
			final Options options ) {
		
		final int nX = ( stopX - startX ) / stepX;
        final int nY = ( stopY - startY ) / stepY;
        
        final ArrayImg<DoubleType, DoubleArray> startingCoordinates = ArrayImgs.doubles( nX, nY, this.zMax - this.zMin );
        final ArrayImg<DoubleType, DoubleArray> startingWeights     = ArrayImgs.doubles( nX, nY, this.zMax - this.zMin );
        
        final ArrayCursor<DoubleType> cCursor = startingCoordinates.cursor();
        final ArrayCursor<DoubleType> wCursor = startingWeights.cursor();
        
        while( cCursor.hasNext() ) {
        	cCursor.fwd();
        	wCursor.fwd();
        	cCursor.get().set( cCursor.getDoublePosition( 2 ) );
        	wCursor.get().set( 1.0 );
        }

		
		return estimate(
				startX, 
				startY, 
				stopX, 
				stopY, 
				stepX, 
				stepY, 
				startingCoordinates, 
				startingWeights, 
				visitor, 
				options);
	}
	
	
	public ArrayImg< DoubleType, DoubleArray > estimate(
			final int startX,
			final int startY,
			final int stopX,
			final int stopY,
			final int stepX,
			final int stepY,
			final ArrayImg<DoubleType, DoubleArray> coordinates,
			final ArrayImg<DoubleType, DoubleArray> weights,
			final LocalVisitor visitor,
			final Options options ) {
		
		final int nX = ( stopX - startX ) / stepX;
        final int nY = ( stopY - startY ) / stepY;
        final int nZ = (int) coordinates.dimension( 3 );
        
        final LUTGrid lutGrid = new LUTGrid( 3, 3, coordinates );
        final ArrayImg<DoubleType, DoubleArray> matrices = ArrayImgs.doubles( nX, nY, nZ, nZ );
        
        for ( int y = 0; y < nY; ++y ) {
        	final IntervalView<DoubleType> ySliced = Views.hyperSlice( matrices, 1, y );
        	final int realY = startY + y*stepY;
        	for ( int x = 0; x < nX; ++x ) {
        		final IntervalView<DoubleType> xSliced = Views.hyperSlice( ySliced, 0, x );
        		final int realX = startX + x*stepX;
        		this.correlationsObject.toMatrix( realX, realY, xSliced );
        	}
        }
        
        final ArrayImg< DoubleType, DoubleArray > previousCoordinates = ArrayImgs.doubles( nX, nY, nZ );
        
        for ( int iteration = 0; iteration < options.nIterations; ++iteration ) {
        	copyDeep( coordinates, previousCoordinates );
        	for ( int y = 0; y < nY; ++y ) {
        		for ( int x = 0; x < nX; ++x ) {
        			estimateCoordinatesAtXY( 
        					matrices,
        					coordinates,
        					previousCoordinates,
        					weights,
        					lutGrid,
        					x,
        					y,
        					options,
        					visitor,
        					iteration );
        		}
        	}
        	
        }
		
		return null;
	}
	
	
	private void estimateCoordinatesAtXY(
			final ArrayImg<DoubleType, DoubleArray> matrices,
			final ArrayImg<DoubleType, DoubleArray> coordinates,
			final ArrayImg<DoubleType, DoubleArray> previousCoordinates,
			final ArrayImg<DoubleType, DoubleArray> weights, 
			final LUTGrid lutGrid, 
			final int x,
			final int y, 
			final Options options, 
			final LocalVisitor visitor, 
			final int iteration) {
		
		final IntervalView<DoubleType> localCoordinates = 
				Views.hyperSlice( Views.hyperSlice( coordinates, 1, y), 0, x );
		final IntervalView<DoubleType> localPreviousCoordinates = 
				Views.hyperSlice( Views.hyperSlice( previousCoordinates, 1, y), 0, x );
		final IntervalView<DoubleType> localWeights = 
				Views.hyperSlice( Views.hyperSlice( weights, 1, y), 0, x );
	}


	public void copyDeep( final ArrayImg< DoubleType, DoubleArray > source, final ArrayImg< DoubleType, DoubleArray > target ) 
	{
		final ArrayCursor<DoubleType> s = source.cursor();
		final ArrayCursor<DoubleType> t = target.cursor();
		while ( s.hasNext() )
			t.next().set( s.next () );
	}
	 

}
