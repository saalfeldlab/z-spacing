package org.janelia.thickness.inference;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.janelia.thickness.EstimateQualityOfSlice;
import org.janelia.thickness.ShiftCoordinates;
import org.janelia.thickness.inference.fits.AbstractCorrelationFit;
import org.janelia.thickness.inference.visitor.LazyVisitor;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.PermutationTransform;
import org.janelia.utility.arrays.ArraySortedIndices;
import org.janelia.utility.arrays.ReplaceNaNs;

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

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class InferFromMatrix
{

	private final AbstractCorrelationFit correlationFit;

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
		public void regularize( double[] coordinates, Options options )
		{
			// do not do anything
		}
	}

	public static abstract class ModelRegularization implements Regularizer
	{
		private final Model< ? > m;

		private final double[] regularizationValues;

		private final double[] weights;

		private final double[] dummy;

		protected ModelRegularization( Model< ? > m, double[] regularizationValues, double[] weights )
		{
			this.m = m;
			this.regularizationValues = regularizationValues;
			this.weights = weights;
			this.dummy = new double[ 1 ];
		}

		protected abstract double[] extractRelevantCoordinates( double[] coordinates );

		@Override
		public void regularize( double[] coordinates, Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
		{
			double[] relevantCoordinates = extractRelevantCoordinates( coordinates );
			m.fit( new double[][] { relevantCoordinates }, new double[][] { regularizationValues }, weights );

			for ( int i = 0; i < coordinates.length; ++i )
			{
				dummy[ 0 ] = coordinates[ i ];
				m.applyInPlace( dummy );
				coordinates[ i ] = dummy[ 0 ];
			}
		}
	}

	public static class BorderRegularization extends ModelRegularization
	{
		private final double[] relevantCoordinates;

		public BorderRegularization( Model< ? > m, int length )
		{
			super( m, new double[] { 0, length - 1 }, new double[] { 1.0, 1.0 } );
			this.relevantCoordinates = new double[ 2 ];
		}

		@Override
		protected double[] extractRelevantCoordinates( double[] coordinates )
		{
			relevantCoordinates[ 0 ] = coordinates[ 0 ];
			relevantCoordinates[ 1 ] = coordinates[ coordinates.length - 1 ];
			return relevantCoordinates;
		}
	}

	public static class IdentityRegularization extends ModelRegularization
	{
		public IdentityRegularization( Model< ? > m, int length )
		{
			super( m, range( 0, length, 1 ), constVals( length, 1.0 ) );
		}

		@Override
		protected double[] extractRelevantCoordinates( double[] coordinates )
		{
			return coordinates;
		}

		public static double[] range( int start, int stop, int step )
		{
			double[] result = new double[ ( stop - start ) / step ];
			for ( int i = 0; i < result.length; ++i, start += step )
				result[ i ] = start;
			return result;
		}

		public static double[] constVals( int length, double val )
		{
			double[] result = new double[ length ];
			for ( int i = 0; i < result.length; ++i )
				result[ i ] = val;
			return result;
		}
	}

	public InferFromMatrix( final AbstractCorrelationFit correlationFit )
	{
		super();

		this.correlationFit = correlationFit;
	}

	public < T extends RealType< T > & NativeType< T > > double[] estimateZCoordinates(
			final RandomAccessibleInterval< T > matrix,
			final double[] startingCoordinates,
			final Options options ) throws Exception
	{
		return estimateZCoordinates(
				matrix,
				startingCoordinates,
				new LazyVisitor(),
				options );
	}

	public < T extends RealType< T > & NativeType< T > > double[] estimateZCoordinates(
			final RandomAccessibleInterval< T > inputMatrix,
			final double[] startingCoordinates,
			final Visitor visitor,
			final Options options ) throws Exception
	{

		final double[] lut = startingCoordinates.clone();
		final int n = ( int ) inputMatrix.dimension( 0 );
		final int[] permutationLut = new int[ n ];
		final int[] inverse = permutationLut.clone();
		final int nMatrixDim = inputMatrix.numDimensions();
		final double[] multipliers = new double[ n ];
		for ( int i = 0; i < multipliers.length; i++ )
		{
			multipliers[ i ] = 1.0;
		}

		final ArrayList< double[] > fitList = new ArrayList< double[] >();
		for ( int i = 0; i < lut.length; ++i )
		{
			fitList.add( new double[ options.comparisonRange ] );
		}

		final ListImg< double[] > localFits = new ListImg< double[] >( fitList, fitList.size() );

		double[] permutedLut = lut.clone(); // sorted lut
		final double[] multipliersPrevious = multipliers.clone();
		ArraySortedIndices.sort( permutedLut, permutationLut, inverse );

		ArrayImg< T, ? > inputMultipliedMatrix = new ArrayImgFactory< T >().create( new long[] { n, n }, inputMatrix.randomAccess().get() );
		for ( Cursor< T > source = Views.flatIterable( inputMatrix ).cursor(), target = Views.flatIterable( inputMultipliedMatrix ).cursor(); source.hasNext(); )
			target.next().set( source.next() );

		final Regularizer regularizer;
		switch ( options.regularizationType )
		{
		case BORDER:
		{
			regularizer = new BorderRegularization( new AffineModel1D(), n );
			break;
		}
		case IDENTITY:
		{
			regularizer = new IdentityRegularization( new AffineModel1D(), n );
			break;
		}
		case NONE:
		{
			regularizer = new NoRegularization();
			break;
		}
		default:
		{
			regularizer = new NoRegularization();
			break;
		}
		}

		for ( int iteration = 0; iteration < options.nIterations; ++iteration )
		{

			// multipliers always in permuted order

			final PermutationTransform permutation = new PermutationTransform( inverse, nMatrixDim, nMatrixDim ); // need
																													// to
																													// create
																													// Transform
																													// into
																													// source?
			final IntervalView< T > matrix = Views.interval( new TransformView< T >( inputMatrix, permutation ), inputMatrix );
			IntervalView< T > multipliedMatrix = Views.interval( new TransformView< T >( inputMultipliedMatrix, permutation ), inputMultipliedMatrix );

			if ( iteration == 0 )
				visitor.act( iteration, matrix, lut, permutationLut, inverse, multipliers, null );

			final double[] shifts = this.getMediatedShifts(
					matrix,
					multipliedMatrix,
					permutedLut,
					multipliers,
					iteration,
					localFits,
					options );

			this.applyShifts(
					permutedLut, // rewrite interface to use view on permuted
									// lut? probably not
					shifts,
					multipliers,
					startingCoordinates,
					permutation.copyToDimension( 1, 1 ),
					options );

			ReplaceNaNs.replace( permutedLut );

			if ( !options.withReorder )
				preventReorder( permutedLut, options ); //

//    		if ( options.withRegularization )
			regularizer.regularize( permutedLut, options );

			updateArray( permutedLut, lut, inverse );
			updateArray( multipliers, multipliersPrevious, inverse );
			permutedLut = lut.clone();
			ArraySortedIndices.sort( permutedLut, permutationLut, inverse );
			updateArray( multipliersPrevious, multipliers, permutationLut );

			visitor.act( iteration + 1, matrix, lut, permutationLut, inverse, multipliers, null );

		}

		return lut;
	}

	public < T extends RealType< T > > double[] getMediatedShifts(
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > multipliedMatrix,
			final double[] lut,
			final double[] multipliers,
			final int iteration,
			final ListImg< double[] > localFits,
			final Options options ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{

		final int nMatrixDimensions = multipliedMatrix.numDimensions();
		final LUTRealTransform transform = new LUTRealTransform( lut, nMatrixDimensions, nMatrixDimensions );

		// use multiplied matrix
		RealRandomAccessible< RealComposite< DoubleType > > fits =
				correlationFit.estimateFromMatrix(
						multipliedMatrix, lut, multipliers, transform, options );
		correlationFit.raster( fits, localFits );

		// use original matrix to estimate multipliers
		EstimateQualityOfSlice.estimateQuadraticFromMatrix( matrix,
				multipliers,
				lut,
				localFits,
				options.multiplierGenerationRegularizerWeight,
				options.comparisonRange,
				options.multiplierEstimationIterations );

		// write multiplied matrix to multipliedMatrix
		RandomAccess< T > matrixRA = matrix.randomAccess();
		RandomAccess< T > multipliedMatrixRA = multipliedMatrix.randomAccess();
		for ( int z = 0; z < lut.length; ++z )
		{
			matrixRA.setPosition( z, 0 );
			multipliedMatrixRA.setPosition( z, 0 );
			int max = Math.min( lut.length, z + options.comparisonRange + 1 );
			for ( int k = Math.max( 0, z - options.comparisonRange ); k < max; ++k )
			{
				matrixRA.setPosition( k, 1 );
				multipliedMatrixRA.setPosition( k, 1 );
				multipliedMatrixRA.get().set( matrixRA.get() );
				if ( k != z )
					multipliedMatrixRA.get().mul( multipliers[ z ] * multipliers[ k ] );
			}
		}

		// use multiplied matrix to collect shifts
		final TreeMap< Long, ArrayList< ValuePair< Double, Double > > > shifts =
				ShiftCoordinates.collectShiftsFromMatrix(
						lut,
						multipliedMatrix,
						multipliers,
						localFits,
						options );

		final double[] mediatedShifts = new double[ lut.length ];
		mediateShifts( shifts, mediatedShifts );
		// this.shiftMediator.mediate( shifts, mediatedShifts );

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

		final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;

		for ( int i = 0; i < coordinates.length; ++i )
		{
			double val = coordinates[ i ];
			val += options.shiftProportion * shifts[ i ];
			val = options.coordinateUpdateRegularizerWeight * regularizerCoordinates[ permutation.applyInverse( i ) ] + inverseCoordinateUpdateRegularizerWeight * val;
			coordinates[ i ] = val;
		}

	}

	public void preventReorder(
			final double[] coordinates,
			final Options options )
	{
		for ( int i = 1; i < coordinates.length; i++ )
		{
			final double previous = coordinates[ i - 1 ];
			if ( previous > coordinates[ i ] )
				coordinates[ i ] = previous + options.minimumSectionThickness;
		}
	}

	public void updateArray( final double[] source, final double[] target, final int[] permutation )
	{
		for ( int i = 0; i < target.length; i++ )
		{
			target[ permutation[ i ] ] = source[ i ];
		}
	}

	public static void mediateShifts(
			Map< Long, ArrayList< ValuePair< Double, Double > > > shifts,
			double[] mediatedShifts )
	{
		for ( int i = 0; i < mediatedShifts.length; ++i )
		{

			final ArrayList< ValuePair< Double, Double > > localShifts = shifts.get( ( long ) i );

			double shift = 0.0;
			double weightSum = 0.0;

			if ( localShifts != null )
			{
				for ( final ValuePair< Double, Double > l : localShifts )
				{
					final Double v = l.getA();
					final Double w = l.getB();
					shift += w * v;
					weightSum += w;
				}
			}

			shift /= weightSum;
			mediatedShifts[ i ] = shift;
		}
	}

}
