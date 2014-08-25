package org.janelia.thickness;

import ij.IJ;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.img.list.ListRandomAccess;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.visitor.LocalVisitor;
import org.janelia.thickness.lut.LUTGrid;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.thickness.normalization.AverageAndStandardDeviationNormalization;
import org.janelia.thickness.normalization.AverageColumnNormalization;
import org.janelia.thickness.normalization.MaxColumnNormalization;
import org.janelia.thickness.normalization.NormalizationInterface;
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

        @Override
        public String toString() {
          final StringBuilder sb = new StringBuilder();
          sb.append("[");
          sb.append(getClass().getName());
          sb.append("]\n");
          for ( final Field f : this.getClass().getDeclaredFields() ) {
        	  sb.append( f.getName() );
        	  sb.append( "\t" );
        	  try {
				final StringBuilder append = sb.append( f.get( this ) );
			} catch (final IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	  sb.append( "\n" );
          }
          
          return sb.toString();
        }

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
        
        final ArrayImg<DoubleType, DoubleArray> matrices = ArrayImgs.doubles( nX, nY, nZ, nZ );
        
       
        final ListImg< RealRandomAccessible<DoubleType>> listMatrices = 
        		new ListImg< RealRandomAccessible<DoubleType > >(
        				new long[] { nX, nY}, 
        				null );
        final ListRandomAccess<RealRandomAccessible<DoubleType>> listRA = listMatrices.randomAccess();
        for ( int y = 0; y < nY; ++y ) {
        	final IntervalView<DoubleType> ySliced = Views.hyperSlice( matrices, 1, y );
        	final int realY = startY + y*stepY;
        	listRA.setPosition( y,  1 );
        	for ( int x = 0; x < nX; ++x ) {
        		final IntervalView<DoubleType> xySliced = Views.hyperSlice( ySliced, 0, x );
        		final int realX = startX + x*stepX;
        		listRA.setPosition( x, 0 );
        		this.correlationsObject.toMatrix( realX, realY, xySliced );
        		final ArrayImg<DoubleType, DoubleArray> localMatrix = ArrayImgs.doubles( nZ + 1, nZ + 1 );
        		this.correlationsObject.toMatrix( realX, realY, localMatrix );
        		final ArrayRandomAccess<DoubleType> localAccess = localMatrix.randomAccess();
        		for ( int d = 0; d < 2; ++d ) {
        			final Cursor<DoubleType> extensionCursor = Views.flatIterable( Views.hyperSlice( localMatrix, d, nZ ) ).cursor();
        			while ( extensionCursor.hasNext() ) {
        				extensionCursor.fwd();
        				final int currPos = extensionCursor.getIntPosition( 0 );
        				localAccess.setPosition( nZ - 1, d );
        				localAccess.setPosition( currPos, 1 - d );
        				extensionCursor.get().set( localAccess.get() );
        			}
        		}
        		final RealRandomAccessible<DoubleType> interpolatedExtended = Views.interpolate( 
        				Views.extendValue(localMatrix, new DoubleType( Double.NaN ) ), 
        				new NLinearInterpolatorFactory<DoubleType>() );
        		listRA.set( interpolatedExtended );
        	}
        }
        
        
        for ( int iteration = 0; iteration < options.nIterations; ++iteration ) {
        	
        	final ArrayImg< DoubleType, DoubleArray > previousCoordinates = ArrayImgs.doubles( nX, nY, nZ );
        	copyDeep( coordinates, previousCoordinates );
        	
        	final ExecutorService executorService = Executors.newFixedThreadPool( options.nThreads );
        	final ArrayList< Callable< Void > > tasks = new ArrayList<Callable< Void > >();
        	
        	
        	for ( int y = 0; y < nY; ++y ) {
        		for ( int x = 0; x < nX; ++x ) {
        			
        			final int finalX = x;
        			final int finalY = y;
        			final int finalIteration = iteration;
        			final LUTGrid lutGridCopy = new LUTGrid( 4, 4, previousCoordinates );
        			tasks.add( new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							estimateCoordinatesAtXY( 
		        					matrices,
		        					coordinates,
		        					previousCoordinates,
		        					weights,
		        					lutGridCopy,
		        					finalX,
		        					finalY,
		        					options,
		        					visitor,
		        					finalIteration );
							return null;
						}
					});
        			
        		}
        	}
        	
        	try {
				executorService.invokeAll( tasks );
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException();
			}
        	executorService.shutdown();
        	
        	NormalizationInterface normalizer;
        	normalizer = new MaxColumnNormalization();
        	normalizer = new AverageColumnNormalization();
        	normalizer = new AverageAndStandardDeviationNormalization();
//        	normalizer.normalize( coordinates );
        	IJ.log( "" + iteration + "/" + options.nIterations );
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
		final IntervalView<DoubleType> localPreviousCoordinates =
				Views.hyperSlice( Views.hyperSlice( previousCoordinates, 1, y ), 0, x );
		final IntervalView<DoubleType> localWeights = 
				Views.hyperSlice( Views.hyperSlice( weights, 1, y), 0, x );
		final IntervalView<DoubleType> localMatrix = 
				Views.hyperSlice( Views.hyperSlice( matrices, 1, y), 0, x );
		
		
		final double[] estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( 
				matrices, 
				localWeights, 
				lutGrid, 
				localPreviousCoordinates,
				options.comparisonRange, 
				this.correlationFitModel.copy(), // copy for thread safety
				x,
				y );
		
		
		final double[] multipliers = EstimateQualityOfSlice.estimateFromMatrix(
				localMatrix,
				localWeights,
				this.measurementsMultiplierModel.copy(), // copy for thread safety
				localPreviousCoordinates,
				mirrorAndExtend( estimatedFit, new NLinearInterpolatorFactory< DoubleType >() ),
				options.multiplierGenerationRegularizerWeight );
		
		final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
				ShiftCoordinates.collectShiftsFromMatrix(
						localPreviousCoordinates,
						localMatrix,
						localWeights,
						multipliers,
						new LUTRealTransform( estimatedFit, 1, 1 ),
						x,
						y);
//		IJ.log( ("AFTER SHIFT" ) );
		
		final ArrayImg< DoubleType, DoubleArray > mediatedShifts = ArrayImgs.doubles( localCoordinates.dimension( 0 ) );
		
		// copy for thread safety
//		IJ.log( "BEFORE MEDIATE" );
		this.shiftMediator.copy().mediate( shifts, mediatedShifts );
//		IJ.log( "AFTER MEDIATE" );
        
        
        final ExtendedRandomAccessibleInterval<DoubleType, ArrayImg<DoubleType, DoubleArray>> extendedPreviousCoordinates = 
        		Views.extendBorder( previousCoordinates );
        
        final Cursor<DoubleType> coordCursor                = Views.flatIterable( localCoordinates ).cursor();
        final ArrayCursor<DoubleType> mediatedCursor        = mediatedShifts.cursor();
        final OutOfBounds<DoubleType> previousAccess        = extendedPreviousCoordinates.randomAccess();
        final RandomAccess<DoubleType> neighborCoordAccess1 = localCoordinates.randomAccess();
        final RandomAccess<DoubleType> neighborCoordAccess2 = localCoordinates.randomAccess();

        
        double previousPositionRegularizer;
        
        final DoubleType dummy = new DoubleType();
        
        final double newPositionVsNeighborWeight = 1.0 - options.neighborRegularizerWeight;
        final double newPositionVsGridWeight     = 1.0 - options.coordinateUpdateRegularizerWeight;
        
        double accumulatedShift = 0.0;
        
        for ( int pos = 0; pos < localCoordinates.dimension( 0 ); ++pos ) {
        	
        	previousPositionRegularizer = 0.0;
        	
        	coordCursor.fwd();
        	mediatedCursor.fwd();
        	neighborCoordAccess1.setPosition( coordCursor );
        	neighborCoordAccess2.setPosition( coordCursor );

        	neighborCoordAccess1.fwd( 0 );
        	neighborCoordAccess2.bck( 0 );
        	
        	final DoubleType c = coordCursor.get();
        	
        	double estimatedRelativeShift = mediatedCursor.get().get() * options.shiftProportion + accumulatedShift;
        	
        	
        	final double fwdVal;
        	if ( pos < localCoordinates.dimension( 0 ) - 1 ) {
        		fwdVal = neighborCoordAccess1.get().get();
        	} else {
        		fwdVal = Double.NaN;
        	}
        	
        	final double bckVal;
        	if ( pos > 0 ) {
        		bckVal = neighborCoordAccess2.get().get();
        	} else {
        		bckVal = Double.NaN;
        	}
        	final double curVal = c.get();
        	
        	
        	final double previousValue = c.get();
        	double tmpNewPosition = previousValue + estimatedRelativeShift;
        	
        	
        	
        	int count = 0;
        	
        	previousAccess.setPosition( pos, 2 );
        	
        	
        	for ( int dXY = -1; dXY <= 1; dXY += 2 ) {
        		previousAccess.setPosition( x, 0 );
        		previousAccess.setPosition( y + dXY, 1 );
        		previousPositionRegularizer += previousAccess.get().get();
        		++count;
        		
        		previousAccess.setPosition( y, 1 );
        		previousAccess.setPosition( x + dXY, 0 );
        		previousPositionRegularizer += previousAccess.get().get();
        		++count;
        	}
        	
        	
        	previousPositionRegularizer /= count;
        	
        	tmpNewPosition = options.coordinateUpdateRegularizerWeight * pos + newPositionVsGridWeight*tmpNewPosition;
        	
        	double diff;
        	if ( pos > 0 && tmpNewPosition < bckVal ) {
        		diff = bckVal - ( curVal + accumulatedShift );
        		estimatedRelativeShift = Math.max( estimatedRelativeShift, diff * options.shiftProportion );
        	} else if ( pos < localCoordinates.dimension( 0 ) - 1 && tmpNewPosition > fwdVal ) {
        		diff = fwdVal - ( curVal + accumulatedShift );
        		estimatedRelativeShift = Math.min( estimatedRelativeShift, diff * options.shiftProportion );
        	} // leave shift untouched otherwise
        	
        	c.set( tmpNewPosition );
        	
        	accumulatedShift = tmpNewPosition - curVal;
        	
        	
        	
//        	final DoubleType c = coordCursor.get();
        	
        	
//        	// shift coordinate as estimated
//        	dummy.set( estimatedRelativeShift + accumulatedShift );
//    		c.add( dummy );
//    		// regularize wrt to z bin
//    		final double zBin = coordCursor.getDoublePosition( 0 );
//    		dummy.set( options.coordinateUpdateRegularizerWeight * zBin );
//    		c.mul( newPositionVsGridWeight );
//    		c.add( dummy );
//    		// regularize wrt to previous neighbor positions
//    		c.mul( newPositionVsNeighborWeight );
//    		dummy.set( previousPositionRegularizer * options.neighborRegularizerWeight );
//    		c.add( dummy );
//    		// need '=' instead of '+=' here, accumulated shift already taken into account
//    		accumulatedShift = c.get() - zBin;
        }
        
	}


	public void copyDeep( final ArrayImg< DoubleType, DoubleArray > source, final ArrayImg< DoubleType, DoubleArray > target ) 
	{
		final ArrayCursor<DoubleType> s = source.cursor();
		final ArrayCursor<DoubleType> t = target.cursor();
		while ( s.hasNext() )
			t.next().set( s.next().get() );
	}
	
	
	public void copyDeep( final ListImg< double[] > source, final ListImg< double[] > target) {
		ListCursor<double[]> s = source.cursor();
		ListCursor<double[]> t = target.cursor();
		
		assert source.numDimensions() == target.numDimensions(): "Source and target dimensions do not agree.";
		for ( int d = 0; d < source.numDimensions(); ++d ) {
			assert source.dimension( d ) == target.dimension( d ): "Source and target dimensions do not agree.";
		}
		assert source.firstElement().length == target.firstElement().length: "Source and target dimensions do not agree.";
		
		while( s.hasNext() ) {
			double[] sArr = s.next();
			double[] tArr = t.next();
			for (int i = 0; i < tArr.length; i++) {
				tArr[i] = sArr[i];
			}
		}
			
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
	 
	
	public static void main( final String[] args ) {
		final Options options = Options.generateDefaultOptions();
		System.out.println( options.toString() );
	}

}
