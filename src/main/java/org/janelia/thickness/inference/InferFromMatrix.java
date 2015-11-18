package org.janelia.thickness.inference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import mpicbg.models.AffineModel1D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

import org.janelia.thickness.EstimateQualityOfSlice;
import org.janelia.thickness.LocalizedCorrelationFit;
import org.janelia.thickness.ShiftCoordinates;
import org.janelia.thickness.cluster.Categorizer;
import org.janelia.thickness.cluster.RangedCategorizer;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.PermutationTransform;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.arrays.ArraySortedIndices;
import org.janelia.utility.tuple.ConstantPair;

public class InferFromMatrix< M extends Model<M> > {

	private final M correlationFitModel;
    private final OpinionMediator shiftMediator;


    public InferFromMatrix(
            final M correlationFitModel,
            final OpinionMediator shiftMediator ) {
            super();

            this.correlationFitModel = correlationFitModel;
            this.shiftMediator = shiftMediator;

           
    }


    public < T extends RealType< T >  & NativeType< T > > double[] estimateZCoordinates(
    		final RandomAccessibleInterval< T > matrix,
    		final double[] startingCoordinates,
    		final Options options
    		) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
            return estimateZCoordinates( 
        		matrix, 
        		startingCoordinates, 
        		new Visitor() {

        			@Override
					public < U extends RealType< U > > void act(final int iteration,
							final RandomAccessibleInterval<U> matrix, final double[] lut,
							final int[] permutation, final int[] inversePermutation,
							final double[] multipliers, final double[] weights,
							final RandomAccessibleInterval< double[] > estimatedFit) {
						// don't do anything
					}
		        	
		        },
		        options );
    }
    
    public < T extends RealType< T > & NativeType< T > > double[] estimateZCoordinates(
            final RandomAccessibleInterval< T > input,
            final double[] startingCoordinates,
            final Visitor visitor,
            final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
    	final RangedCategorizer categorizer = new RangedCategorizer( startingCoordinates.length );
    	return estimateZCoordinates( input, startingCoordinates, visitor, categorizer, options );
    }


    public < T extends RealType< T > & NativeType< T >> double[] estimateZCoordinates(
            final RandomAccessibleInterval< T > inputMatrix,
            final double[] startingCoordinates,
            final Visitor visitor,
            final Categorizer categorizer,
            final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
    	
    	final double[] lut         = startingCoordinates.clone();
    	final int n                = (int) inputMatrix.dimension( 0 );
    	final int[] permutationLut = new int[ n ];
    	final int[] inverse        = permutationLut.clone();
    	final int nMatrixDim       = inputMatrix.numDimensions();
    	final double[] weights     = new double[ n ];
    	for (int i = 0; i < weights.length; i++) {
			weights[ i ] = 1.0;
		}
    	final double multipliers[] = weights.clone();
    	
    	final ArrayList<double[]> fitList = new ArrayList< double[] >();
		for ( int i = 0; i < lut.length; ++i ) {
			fitList.add( new double[ options.comparisonRange ] );
		}
		
		final ListImg< double[] > localFits = new ListImg<double[]>( fitList, fitList.size() );
		
		double[] permutedLut         = lut.clone(); // sorted lut
		final double[] multipliersPrevious = multipliers.clone();
		final double[] weightsPrevious     = weights.clone();
		ArraySortedIndices.sort( permutedLut, permutationLut, inverse );

		ArrayImg<T, ?> inputMultipliedMatrix = new ArrayImgFactory<T>().create(new long[]{n, n}, inputMatrix.randomAccess().get());
		for( Cursor< T > source = Views.flatIterable( inputMatrix ).cursor(), target = Views.flatIterable( inputMultipliedMatrix ).cursor(); source.hasNext(); )
			target.next().set( source.next() );

//		ImageJFunctions.show( inputMultipliedMatrix );
    	
    	for ( int iteration = 0; iteration < options.nIterations; ++iteration ) {

			// multipliers always in permuted order
    		
    		final PermutationTransform permutation       = new PermutationTransform( inverse, nMatrixDim, nMatrixDim ); // need to create Transform into source?
    		final IntervalView<T> matrix                 = Views.interval( new TransformView< T >( inputMatrix, permutation ), inputMatrix );
			IntervalView<T> multipliedMatrix             = Views.interval(new TransformView<T>(inputMultipliedMatrix, permutation), inputMultipliedMatrix);
//    		final IntervalView<DoubleType> currentLutImg = Views.interval( new TransformView< DoubleType >( ArrayImgs.doubles( lut, n ), permutation ), new FinalInterval( n ) ); // use this?
    		
    		if ( iteration == 0 )
    			visitor.act( iteration, matrix, lut, permutationLut, inverse, multipliers, weights, null );
    		
    		final double[] shifts = this.getMediatedShifts(
					matrix,
    				multipliedMatrix,
    				permutedLut, 
    				multipliers, 
    				weights, 
    				iteration, 
    				categorizer, 
    				localFits,
    				options);
    		
    		this.applyShifts( 
    				permutedLut, // rewrite interface to use view on permuted lut? probably not
    				shifts, 
    				multipliers,
    				startingCoordinates,
    				permutation.copyToDimension( 1, 1 ), 
    				options);
    		
    		if ( !options.withReorder )
    			preventReorder( permutedLut, options ); // 
    		
    		if ( options.withRegularization )
    			regularize( permutedLut, options );

    		updateArray( permutedLut, lut, inverse );
    		updateArray( multipliers, multipliersPrevious, inverse );
    		updateArray( weights, weightsPrevious, inverse );
    		permutedLut = lut.clone();
    		ArraySortedIndices.sort( permutedLut, permutationLut, inverse );
    		updateArray( multipliersPrevious, multipliers, permutationLut );
    		updateArray( weightsPrevious, weights, permutationLut );

    	}
    	
    	return lut;
    }
        	

    public < T extends RealType< T > > double[] getMediatedShifts (
			final RandomAccessibleInterval< T > matrix,
    		final RandomAccessibleInterval< T > multipliedMatrix,
    		final double[] lut,
    		final double[] multipliers,
            final double[] weights,
            final int iteration,
            final Categorizer categorizer,
            final ListImg< double[] > localFits,
            final Options options
            ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

    	final int nMatrixDimensions      = multipliedMatrix.numDimensions();
    	final LUTRealTransform transform = new LUTRealTransform( lut, nMatrixDimensions, nMatrixDimensions );

		// use multiplied matrix before warping!
		LocalizedCorrelationFit.estimateFromMatrix( multipliedMatrix, lut, transform, options.comparisonRange, correlationFitModel, localFits, options.forceMontonicity );

		// use original matrix to estimate multipliers
		EstimateQualityOfSlice.estimateQuadraticFromMatrix( matrix,
				weights,
				multipliers,
				lut,
				localFits,
				options.multiplierGenerationRegularizerWeight,
				options.comparisonRange,
				options.multiplierEstimationIterations );

		// write multiplied matrix to multipliedMatrix
		RandomAccess<T> matrixRA = matrix.randomAccess();
		RandomAccess<T> multipliedMatrixRA = multipliedMatrix.randomAccess();
		for( int z = 0; z < lut.length; ++z )
		{
			matrixRA.setPosition( z, 0 );
			multipliedMatrixRA.setPosition( z, 0 );
			int max = Math.min(lut.length, z + options.comparisonRange + 1);
			for ( int k = Math.max( 0, z - options.comparisonRange ); k < max; ++k )
			{
				matrixRA.setPosition( k, 1 );
				multipliedMatrixRA.setPosition( k, 1 );
				multipliedMatrixRA.get().set( matrixRA.get() );
				if ( z != k )
					multipliedMatrixRA.get().mul( multipliers[z]*multipliers[k] );
			}
		}

		// use multiplied matrix to collect shifts
		final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
		            ShiftCoordinates.collectShiftsFromMatrix(
		                    lut,
		                    multipliedMatrix,
		                    weights,
		                    multipliers,
		                    localFits );
		
		final double[] mediatedShifts = new double[ lut.length ];
		this.shiftMediator.mediate( shifts, mediatedShifts );
		
		return mediatedShifts;
    }
        
        
    public void applyShifts( 
    		final double[] coordinates, 
    		final double[] shifts, 
    		final double[] multipliers,
    		final double[] regularizerCoordinates,
    		final PermutationTransform permutation, 
    		final Options options ) 
    {
		final double[] smoothedShifts = shifts;//new double[ shifts.length ];
		
		final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;
		
	    
		final double accumulatedCorrections = 0.0;
		for ( int ijk = 0; ijk < coordinates.length; ++ijk ) {
			double val = coordinates[ ijk ];
		    val += accumulatedCorrections + options.shiftProportion * smoothedShifts[ ijk ];
		    val = options.coordinateUpdateRegularizerWeight * regularizerCoordinates[ permutation.applyInverse( ijk ) ] + inverseCoordinateUpdateRegularizerWeight * val;
		    coordinates[ ijk ] = val;
		}
		
    }
    
    
    public void preventReorder( 
    		final double[] coordinates, 
    		final Options options ) {
    	for (int i = 1; i < coordinates.length; i++) {
    		final double previous = coordinates[ i - 1];
			if ( previous > coordinates[ i ] )
				coordinates[ i ] = previous + options.minimumSectionThickness;
		}
    }
			
			
    public void regularize( 
		final double[] coordinates,
		final Options options ) 
    {
		final float[] floatLut = new float[ 2 ]; final float[] arange = new float[ 2 ];
		final float[] floatWeights = new float[ 2 ];
		for (int i = 0; i < arange.length; i++) {
			arange[i] = i;
			floatWeights[i] = 1.0f;
		}
		final int maxIndex = coordinates.length - 1;
		floatLut[ 0 ]       = (float) coordinates[ 0 ];
		arange[ 0 ]         = 0f;
		floatWeights[ 0 ]   = 1.0f;
		floatLut[ 1 ]       = (float) coordinates[ maxIndex ];
		arange[ 1 ]         = maxIndex;
		floatWeights[ 1 ] = 1.0f;
		
		final AffineModel1D coordinatesFitModel = new AffineModel1D();
		
		
		try {
			coordinatesFitModel.fit( new float[][]{floatLut}, new float[][]{arange}, floatWeights );
			final double[] affineArray = new double[ 2 ];
			coordinatesFitModel.toArray( affineArray );
			
			for ( int i = 0; i < coordinates.length; ++i ) {
				coordinates[i] = affineArray[0] * coordinates[i] + affineArray[1];
			}
		} catch (final NotEnoughDataPointsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllDefinedDataPointsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
    
    
    public void updateArray( final double[] source, final double[] target, final int[] permutation ) {
    	for (int i = 0; i < target.length; i++) {
			target[ permutation[ i ] ] = source[ i ];
		}
    }
	
}

