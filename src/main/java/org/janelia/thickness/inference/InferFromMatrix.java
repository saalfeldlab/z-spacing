package org.janelia.thickness.inference;

import mpicbg.models.AffineModel1D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.RealComposite;
import org.janelia.thickness.EstimateQualityOfSlice;
import org.janelia.thickness.ShiftCoordinates;
import org.janelia.thickness.inference.fits.AbstractCorrelationFit;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.PermutationTransform;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.arrays.ArraySortedIndices;
import org.janelia.utility.arrays.ReplaceNaNs;

import java.util.ArrayList;
import java.util.TreeMap;

public class InferFromMatrix {

	private final AbstractCorrelationFit correlationFit;
    private final OpinionMediator shiftMediator;

	public enum RegularizationType
	{
		NONE,
		IDENTITY,
		BORDER
	}

	public static interface Regularizer
	{
		public void regularize( double[] coordinates, Options options ) throws Exception;
	}

	public static class NoRegularization implements Regularizer
	{

		@Override
		public void regularize(double[] coordinates, Options options) {
			// do not do anything
		}
	}

	public static abstract class ModelRegularization implements Regularizer{
		private final Model< ? > m;
		private final double[] regularizationValues;
		private final double[] weights;
		private final double[] dummy;

		protected ModelRegularization(Model<?> m, double[] regularizationValues, double[] weights) {
			this.m = m;
			this.regularizationValues = regularizationValues;
			this.weights = weights;
			this.dummy = new double[ 1 ];
		}

		protected abstract double[] extractRelevantCoordinates( double[] coordinates );

		@Override
		public void regularize( double[] coordinates, Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
			double[] relevantCoordinates = extractRelevantCoordinates(coordinates);
			m.fit( new double[][] { relevantCoordinates }, new double[][] { regularizationValues }, weights );


			for ( int i = 0; i < coordinates.length; ++i ) {
				dummy[0] = coordinates[i];
				m.applyInPlace( dummy );
				coordinates[i] = dummy[0];
			}
		}
	}

	public static class BorderRegularization extends ModelRegularization
	{
		private final double[] relevantCoordinates;

		public BorderRegularization(Model<?> m, int length) {
			super( m, new double[] { 0, length - 1 }, new double[] { 1.0, 1.0 } );
			this.relevantCoordinates = new double[ 2 ];
		}

		@Override
		protected double[] extractRelevantCoordinates(double[] coordinates) {
			relevantCoordinates[ 0 ] = coordinates[ 0 ];
			relevantCoordinates[ 1 ] = coordinates[ coordinates.length - 1 ];
			return relevantCoordinates;
		}
	}

	public static class IdentityRegularization extends ModelRegularization
	{
		public IdentityRegularization(Model<?> m, int length) {
			super( m, range( 0, length, 1 ), constVals( length, 1.0 ) );
		}

		@Override
		protected double[] extractRelevantCoordinates(double[] coordinates) {
			return coordinates;
		}

		public static double[] range( int start, int stop, int step )
		{
			double[] result = new double[(stop - start) / step];
			for ( int i = 0; i < result.length; ++i, start += step )
				result[ i ] = start;
			return result;
		}

		public static double[] constVals( int length, double val )
		{
			double[] result = new double[length];
			for ( int i = 0; i < result.length; ++i )
				result[i] = val;
			return result;
		}
	}


    public InferFromMatrix(
            final AbstractCorrelationFit correlationFit,
            final OpinionMediator shiftMediator ) {
            super();

            this.correlationFit = correlationFit;
            this.shiftMediator = shiftMediator;

           
    }


    public < T extends RealType< T >  & NativeType< T > > double[] estimateZCoordinates(
    		final RandomAccessibleInterval< T > matrix,
    		final double[] startingCoordinates,
    		final Options options
    		) throws Exception {
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

    public < T extends RealType< T > & NativeType< T >> double[] estimateZCoordinates(
            final RandomAccessibleInterval< T > inputMatrix,
            final double[] startingCoordinates,
            final Visitor visitor,
            final Options options) throws Exception {
    	
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

		final Regularizer regularizer;
		switch ( options.regularizationType )
		{
			case BORDER: {
				regularizer = new BorderRegularization(new AffineModel1D(), n);
				break;
			}
			case IDENTITY: {
				regularizer = new IdentityRegularization(new AffineModel1D(), n);
				break;
			}
			case NONE: {
				regularizer = new NoRegularization();
				break;
			}
			default: {
				regularizer = new NoRegularization();
				break;
			}
		}

    	for ( int iteration = 0; iteration < options.nIterations; ++iteration ) {

			// multipliers always in permuted order
    		
    		final PermutationTransform permutation       = new PermutationTransform( inverse, nMatrixDim, nMatrixDim ); // need to create Transform into source?
    		final IntervalView<T> matrix                 = Views.interval( new TransformView< T >( inputMatrix, permutation ), inputMatrix );
			IntervalView<T> multipliedMatrix             = Views.interval( new TransformView<T>(inputMultipliedMatrix, permutation), inputMultipliedMatrix );

    		
    		if ( iteration == 0 )
				visitor.act( iteration, matrix, lut, permutationLut, inverse, multipliers, weights, null );
    		
    		final double[] shifts = this.getMediatedShifts(
					matrix,
    				multipliedMatrix,
    				permutedLut, 
    				multipliers, 
    				weights, 
    				iteration,
    				localFits,
    				options);

    		this.applyShifts( 
    				permutedLut, // rewrite interface to use view on permuted lut? probably not
    				shifts, 
    				multipliers,
    				startingCoordinates,
    				permutation.copyToDimension( 1, 1 ), 
    				options);

			ReplaceNaNs.replace( permutedLut );

    		if ( !options.withReorder )
    			preventReorder( permutedLut, options ); // 
    		
//    		if ( options.withRegularization )
			regularizer.regularize( permutedLut, options );

    		updateArray( permutedLut, lut, inverse );
    		updateArray( multipliers, multipliersPrevious, inverse );
    		updateArray( weights, weightsPrevious, inverse );
    		permutedLut = lut.clone();
    		ArraySortedIndices.sort( permutedLut, permutationLut, inverse );
    		updateArray( multipliersPrevious, multipliers, permutationLut );
    		updateArray( weightsPrevious, weights, permutationLut );

			visitor.act( iteration+1, matrix, lut, permutationLut, inverse, multipliers, weights, null );

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
            final ListImg< double[] > localFits,
            final Options options
            ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

    	final int nMatrixDimensions      = multipliedMatrix.numDimensions();
    	final LUTRealTransform transform = new LUTRealTransform( lut, nMatrixDimensions, nMatrixDimensions );

        // use multiplied matrix
		RealRandomAccessible<RealComposite<DoubleType>> fits =
				correlationFit.estimateFromMatrix(
						multipliedMatrix, lut, weights, multipliers, transform, options);
		correlationFit.raster( fits, localFits );

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
				if ( k != z )
					multipliedMatrixRA.get().mul( multipliers[z]*multipliers[k] );
			}
		}

		// use multiplied matrix to collect shifts
		final TreeMap< Long, ArrayList<ValuePair< Double, Double >> > shifts =
		            ShiftCoordinates.collectShiftsFromMatrix(
		                    lut,
		                    multipliedMatrix,
		                    weights,
		                    multipliers,
		                    localFits,
							options );
		
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
			
    public void updateArray( final double[] source, final double[] target, final int[] permutation ) {
    	for (int i = 0; i < target.length; i++) {
			target[ permutation[ i ] ] = source[ i ];
		}
    }
	
}

