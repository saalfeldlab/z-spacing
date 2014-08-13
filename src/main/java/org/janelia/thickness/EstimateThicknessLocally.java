package org.janelia.thickness;

import ij.IJ;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.visitor.LocalVisitor;
import org.janelia.thickness.lut.LUTGrid;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.ConstantPair;


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
		this.zMin = this.correlationsObject.getzMin(); // Collections.min( this.correlationsObject.getMetaMap().keySet() );
        this.zMax = this.correlationsObject.getzMax(); // Collections.max( this.correlationsObject.getMetaMap().keySet() );
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
			final Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		return estimate(
				startX, 
				startY, 
				stopX, 
				stopY, 
				stepX, 
				stepY, 
				new LocalVisitor() {
				}, 
				options);
	}
	
	public ArrayImg< DoubleType, DoubleArray > estimate(
			final int startX,
			final int startY,
			final int stopX,
			final int stopY,
			final int stepX,
			final int stepY,
			final LocalVisitor visitor,
			final Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
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
			final Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final int nX = ( stopX - startX ) / stepX;
        final int nY = ( stopY - startY ) / stepY;
        final int nZ = (int) coordinates.dimension( 2 );
        
        final LUTGrid lutGrid = new LUTGrid( 4, 4, coordinates );
        final ArrayImg<DoubleType, DoubleArray> matrices = ArrayImgs.doubles( nX, nY, nZ, nZ );
        
        for ( int y = 0; y < nY; ++y ) {
        	final IntervalView<DoubleType> ySliced = Views.hyperSlice( matrices, 1, y );
        	final int realY = startY + y*stepY;
        	for ( int x = 0; x < nX; ++x ) {
        		final IntervalView<DoubleType> xySliced = Views.hyperSlice( ySliced, 0, x );
        		final int realX = startX + x*stepX;
        		this.correlationsObject.toMatrix( realX, realY, xySliced );
        	}
        }
        
        final ArrayImg< DoubleType, DoubleArray > previousCoordinates = ArrayImgs.doubles( nX, nY, nZ );
        
        for ( int iteration = 0; iteration < options.nIterations; ++iteration ) {
        	copyDeep( coordinates, previousCoordinates );
        	for ( int y = 0; y < nY; ++y ) {
        		IJ.log( String.format( "y=%d/%d", y, nY ));
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
		
		return coordinates;
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
			final int iteration) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final IntervalView<DoubleType> localCoordinates = 
				Views.hyperSlice( Views.hyperSlice( coordinates, 1, y), 0, x );
		final IntervalView<DoubleType> localWeights = 
				Views.hyperSlice( Views.hyperSlice( weights, 1, y), 0, x );
		final IntervalView<DoubleType> localMatrix = 
				Views.hyperSlice( Views.hyperSlice( matrices, 1, y), 0, x );
		
		
		final double[] variances = new double[ options.comparisonRange ];
		
		final double[] estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( 
				matrices, 
				localWeights, 
				lutGrid, 
				localCoordinates,
				options.comparisonRange, 
				this.correlationFitModel, 
				variances,
				x,
				y );
		
		
		final double[] multipliers = EstimateQualityOfSlice.estimateFromMatrix(
				localMatrix,
				localWeights,
				this.measurementsMultiplierModel,
				localCoordinates,
				mirrorAndExtend( estimatedFit, new NLinearInterpolatorFactory< DoubleType >() ),
				options.multiplierGenerationRegularizerWeight );
		
		final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
				ShiftCoordinates.collectShiftsFromMatrix(
						localCoordinates,
						matrices,
						localWeights,
						multipliers,
						lutGrid,
						x,
						y);
		
		final ArrayImg< DoubleType, DoubleArray > mediatedShifts = ArrayImgs.doubles( localCoordinates.dimension( 0 ) );
		
		this.shiftMediator.mediate( shifts, mediatedShifts );
		
        
        
        final ExtendedRandomAccessibleInterval<DoubleType, ArrayImg<DoubleType, DoubleArray>> extendedPreviousCoordinates = 
        		Views.extendBorder( previousCoordinates );
        
        final Cursor<DoubleType> coordCursor               = Views.flatIterable( localCoordinates ).cursor();
        final ArrayCursor<DoubleType> mediatedCursor       = mediatedShifts.cursor();
        final OutOfBounds<DoubleType> previousAccess = extendedPreviousCoordinates.randomAccess();
        
        double previousPositionRegularizer;
        
        final DoubleType dummy = new DoubleType();
        
        final double newPositionWeight = 1.0 - options.coordinateUpdateRegularizerWeight;
        
        for ( int pos = 0; pos < localCoordinates.dimension( 0 ); ++pos ) {
        	
        	previousPositionRegularizer = 0.0;
        	
        	coordCursor.fwd();
        	mediatedCursor.fwd();
        	
        	previousAccess.setPosition( pos, 2 );
        	for ( int dy = -1; dy <= 1; ++dy ) {
        		previousAccess.setPosition( y + dy, 1 );
        		for ( int dx = -1; dx <= 1; ++dx ) {
        			previousAccess.setPosition( x + dx, 0 );
        			previousPositionRegularizer += previousAccess.get().get();
        		}
        	}
        	previousPositionRegularizer /= 9.0;
        	dummy.set( mediatedCursor.get().get() * options.shiftProportion );
    		coordCursor.get().add( dummy );
    		coordCursor.get().mul( newPositionWeight );
    		dummy.set( previousPositionRegularizer * options.coordinateUpdateRegularizerWeight );
    		coordCursor.get().add( dummy );
        }
        
	}


	public void copyDeep( final ArrayImg< DoubleType, DoubleArray > source, final ArrayImg< DoubleType, DoubleArray > target ) 
	{
		final ArrayCursor<DoubleType> s = source.cursor();
		final ArrayCursor<DoubleType> t = target.cursor();
		while ( s.hasNext() )
			t.next().set( s.next () );
	}
	
	private static RealRandomAccessible< DoubleType > mirrorAndExtend(
            final double[] data,
            final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory )
    {
            final ArrayImg< DoubleType, DoubleArray > img = ArrayImgs.doubles( data, new long[]{ data.length } );
            final RandomAccessible< DoubleType > mirror = Views.extendMirrorSingle( img );
            final RandomAccessibleInterval< DoubleType > crop = Views.interval( mirror, new long[]{ 1 - data.length }, new long[]{ data.length - 1 } );
            final RandomAccessible< DoubleType > extension = Views.extendValue( crop, new DoubleType( Double.NaN ) );
            return Views.interpolate( extension, interpolatorFactory );
    }
	 

}
