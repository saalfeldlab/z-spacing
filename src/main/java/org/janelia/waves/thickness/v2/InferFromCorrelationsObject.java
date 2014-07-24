package org.janelia.waves.thickness.v2;

import ij.ImageJ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.TranslationModel1D;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.models.ScaleModel;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.correlations.DummyCorrelationsObject;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;
import org.janelia.waves.thickness.v2.inference.visitor.Visitor;
import org.janelia.waves.thickness.v2.mediator.OpinionMediator;
import org.janelia.waves.thickness.v2.mediator.OpinionMediatorModel;

public class InferFromCorrelationsObject< M extends Model<M>, L extends Model<L> > {
	
	private final CorrelationsObjectInterface correlationsObject;
	private final int nIterations;
	private final int comparisonRange;
	private final M correlationFitModel;
	private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> lutInterpolatorFactory;
	private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory;
	private final L measurementsMultiplierModel;
	private final int nThreads;
	private final OpinionMediator shiftMediator;
	private final long zMin;
	private final long zMax;
	
	
	
	
	
	public InferFromCorrelationsObject(
			final CorrelationsObjectInterface correlationsObject,
			final int nIterations,
			final int comparisonRange,
			final M correlationFitModel,
			final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> lutInterpolatorFactory,
			final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory,
			final L measurementsMultiplierModel,
			final int nThreads,
			final OpinionMediator shiftMediator ) {
		super();
		
		this.correlationsObject = correlationsObject;
		this.nIterations = nIterations;
		this.comparisonRange = comparisonRange;
		this.correlationFitModel = correlationFitModel;
		this.lutInterpolatorFactory = lutInterpolatorFactory;
		this.fitInterpolatorFactory = fitInterpolatorFactory;
		this.measurementsMultiplierModel = measurementsMultiplierModel;
		this.nThreads = nThreads;
		this.shiftMediator = shiftMediator;
		
		final Iterator<Long> iterator = this.correlationsObject.getMetaMap().keySet().iterator();
		zMin = iterator.next();
		long zMaxTmp = zMin;
		
		while ( iterator.hasNext() )
			zMaxTmp = iterator.next();
		zMax = zMaxTmp + 1;
	}
	
	public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates( final long x, final long y, final double[] startingCoordinates ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		return estimateZCoordinates( x, y, startingCoordinates, new Visitor() {

			public void act(final int iteration,
					final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
					final LUTRealTransform transform,
					final ArrayImg<DoubleType, DoubleArray> multipliers,
					final ArrayImg<DoubleType, DoubleArray> weights,
					final FitWithGradient fitWithGradient) {
				// don't do anything
			}} );
	}
	
	public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates( final long x, final long y, final double[] startingCoordinates, final Visitor visitor ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final ArrayImg<DoubleType, DoubleArray> matrix = this.correlationsToMatrix( x, y);
		final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( matrix.dimension( 0 ) );
		
		for ( final DoubleType w : weights) {
			w.set( 1.0 );
		}
		
		final double[] lut = startingCoordinates;
		final double[] coordinateArr = lut;
		
		
		
		
		
		final ArrayImg<DoubleType, DoubleArray> coordinates = ArrayImgs.doubles( coordinateArr, coordinateArr.length );
		
//		{ 
//			int i = 0; 
//			for ( final DoubleType c : coordinates ) {
//				c.set( startingCoordinates[i]);
//				++i;
//			}
//		}
		
		final LUTRealTransform transform = new LUTRealTransform(lut, this.lutInterpolatorFactory, matrix.numDimensions(), matrix.numDimensions() );
		
		final ArrayImg<DoubleType, DoubleArray> mediatedShifts = ArrayImgs.doubles( lut.length );
		

		
		ArrayCursor<DoubleType> mediatedCursor   = mediatedShifts.cursor();
		ArrayCursor<DoubleType> coordinateCursor = coordinates.cursor();
		
		EstimateCorrelationsAtSamplePoints.arryImg = ArrayImgs.doubles( 11, 120, this.nIterations );
		for ( int n = 0; n < this.nIterations; ++n ) {
			final double[] vars = new double[ this.comparisonRange ];
			
			EstimateCorrelationsAtSamplePoints.t = n;
			final ArrayImg<DoubleType, DoubleArray> estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( matrix, weights, transform, coordinateArr, this.comparisonRange, this.correlationFitModel, vars );
			
			
			final File f = new File( String.format( "/groups/saalfeld/home/hanslovskyp/fit_iteration=%d.csv", n ) );
			try {
				f.createNewFile();
				final FileWriter fw = new FileWriter( f.getAbsoluteFile() );
				final BufferedWriter bw = new BufferedWriter( fw );
				final ArrayCursor<DoubleType> efCursor = estimatedFit.cursor();
				
				for (int i = 0; i < vars.length; i++) {
					bw.write( String.format( "%d,%f,%f\n", i, efCursor.next().get(), vars[i] ) );
				}
				
				bw.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			final FitWithGradient fitWithGradient = new FitWithGradient( estimatedFit, new FitWithGradient.SymmetricGradient(), this.fitInterpolatorFactory );
			
			
			final ArrayImg<DoubleType, DoubleArray> multipliers = EstimateQualityOfSlice.estimateFromMatrix( matrix, 
					weights, 
					this.measurementsMultiplierModel, 
					coordinates, 
					fitWithGradient.getFit(), 
					this.nThreads);
	
			final File f2 = new File( String.format( "/groups/saalfeld/home/hanslovskyp/mult_iteration=%d.csv", n ) );
			try {
				f2.createNewFile();
				final FileWriter fw = new FileWriter( f2.getAbsoluteFile() );
				final BufferedWriter bw = new BufferedWriter( fw );
				final ArrayCursor<DoubleType> mCursor = multipliers.cursor();
				
				for (int i = 0; i < multipliers.dimension( 0 ); i++) {
					bw.write( String.format( "%d,%f\n", i, mCursor.next().get() ) );
				}
				
				bw.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			final TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts = ShiftCoordinates.collectShiftsFromMatrix(coordinates, 
					matrix, 
					weights, 
					multipliers, 
					fitWithGradient.getFit(),
					fitWithGradient.getGradient() );
			
			
			
			
			
			this.shiftMediator.mediate( shifts, mediatedShifts );
			
			
			
			mediatedCursor   = mediatedShifts.cursor();
			coordinateCursor = coordinates.cursor();
			
			
			int ijk = 0;
			
			final File file = new File( String.format( "/groups/saalfeld/home/hanslovskyp/shifts_iteration=%d.csv", n ) );
			try {
				file.createNewFile();
				final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
				final BufferedWriter bw = new BufferedWriter( fw );
				while ( mediatedCursor.hasNext() ) {
					
					coordinateCursor.fwd();
					mediatedCursor.fwd();
//					coordinateCursor.get().setReal( coordinateCursor.get().getRealDouble() + 0.1*mediatedCursor.get().getRealDouble() );
					
					
					lut[ijk] += 0.5 * mediatedCursor.get().get();
					lut[ijk] *= 0.99;
					lut[ijk] += 0.01 * ijk;
					
					bw.write( String.format( "%d,%f,%f\n", ijk, mediatedCursor.get().get(), coordinateCursor.get().get() ) );
					++ijk;
					
				}
				
				bw.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			transform.update( lut );
			visitor.act( n, matrix, lut, transform, multipliers, weights, fitWithGradient);
			
		}
		ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.arryImg );
		return coordinates;
	}
	
	
	public ArrayImg< DoubleType, DoubleArray > correlationsToMatrix( final long x, final long y ) {
		
		final int nSlices = this.correlationsObject.getMetaMap().size(); 
		final ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( nSlices, nSlices );
		for ( final DoubleType m : matrix ) {
			m.set( Double.NaN );
		}
		
		for ( long zRef = this.zMin; zRef < this.zMax; ++zRef ) {
			final long relativeZ = zRef - this.zMin;
			final RandomAccessibleInterval<DoubleType> correlations = this.correlationsObject.extractDoubleCorrelationsAt( x, y, zRef ).getA();
			final IntervalView<DoubleType> row = Views.hyperSlice( matrix, 1, relativeZ);
			
			final RandomAccess<DoubleType> correlationsAccess = correlations.randomAccess();
			final RandomAccess<DoubleType> rowAccess          = row.randomAccess();
			
			final Meta meta = this.correlationsObject.getMetaMap().get( zRef );
			
			rowAccess.setPosition( Math.max( meta.zCoordinateMin - this.zMin, 0 ), 0 );
			
			for ( long zComp = meta.zCoordinateMin; zComp < meta.zCoordinateMax; ++zComp ) {
				if ( zComp < this.zMin || zComp >= this.zMax ) {
					correlationsAccess.fwd( 0 );
					continue;
				}
				rowAccess.get().set( correlationsAccess.get() );
				rowAccess.fwd( 0 );
				correlationsAccess.fwd( 0 );
				
			}
			
		}
		 
		// remove this as soon everything's working, only for visualization purposes
//		EstimateCorrelationsAtSamplePoints.matrixImg = ArrayImgs.doubles( matrix.dimension(0) * 5, matrix.dimension(1) * 5, this.nIterations );
//		EstimateCorrelationsAtSamplePoints.matrixImg = ArrayImgs.doubles( 512, 512, this.nIterations );
		// end vis
		
//		final Random rng = new Random( 100 );
//		
//		for ( long i = 0; i < matrix.dimension(0); ++ i ) {
//			
//			final double mult = 1.0;//rng.nextDouble() / 2 + 0.5;
//			
//			
//			final Cursor<DoubleType> sl1 = Views.flatIterable( Views.hyperSlice(matrix, 0, i) ).cursor();
//			final Cursor<DoubleType> sl2 = Views.flatIterable( Views.hyperSlice(matrix, 1, i) ).cursor();
//			
//			while ( sl1.hasNext() ) {
//				sl1.next().mul( mult );
//				sl2.next().mul( mult );
//			}
//		}
		
		return matrix;
	}
	
	
	public static void main(final String[] args) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		
		
		final Random rng = new Random( 100 );

		final boolean doPrint = false;
		
		final int nData = 200;
		final int zMin  = 1;
		final int zMax  = zMin + nData;
		final double xScale   = 0.5;
		final double sigma    = 4.0;
		final int range = 25;
		final double gradient = -1.0 / range;
		final int nRep = 40;
		
		final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
		final ArrayList<Double> coordinateShift = new ArrayList<Double>();
		final ArrayList<Double> zShifts         = new ArrayList<Double>();
		
		
		for ( int n = 0; n < nData; ++n ) {
			coordinateBase.add( (double) (n + 1) );
			zShifts.add( 0.0 );
			coordinateShift.add( (double) (n + 1) );
		}
		
		
		double prev = 0.0;
		for ( int n = range; n < nData - range; ++n ) {
			coordinateBase.set(n, (double) (n + 1) );
			zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
			coordinateShift.set( n, Math.max( prev, coordinateBase.get(n) ) + zShifts.get(n) );
			prev = coordinateShift.get( n );			
		}
		
		
		if ( doPrint  ) {
			System.out.println( coordinateBase );
			System.out.println( zShifts );
			System.out.println( coordinateShift );
		}
		
		final double[] initialCoordinates = new double[ nData - 2*range ];
		
		final File file = new File("/groups/saalfeld/home/hanslovskyp/initialcoordinates.csv");
		try {
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
		
			for ( int i = 0; i < initialCoordinates.length; ++i ) {
				initialCoordinates[i] = coordinateShift.get( range + i ) - range - 1;
				final String writeString = "" + i + "," + initialCoordinates[i] + "\n";
				bw.write( writeString );
			}
			bw.close();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		final TreeMap< Long, Meta > metaMap = new TreeMap<Long, Meta>();
		
		for ( int i = range; i < nData - range; ++i ) {
			
//			System.out.println( i );
			final ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
			final ArrayCursor<DoubleType> m = measure.cursor();
			for ( int r = - range; r <= range; ++r ) {
				m.next().set( new BellCurve().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), sigma } ) );
//				m.next().set( new AbsoluteLinear().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), 1.0, gradient } ) );
//				m.next().set( Math.abs(coordinateShift.get( i + r ) -  coordinateShift.get( i ) ) * gradient + 1.0 );
//				System.out.println( i + r + " " + m.get().get() );
			}
		
			
			final ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
			final ArrayCursor<DoubleType> c = coord.cursor();
			for ( int r = - range; r <= range; ++r ) {
				c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
			}

			
			
			corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i) ),
					new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));

			final Meta meta = new Meta();
			meta.zPosition = i;
			meta.zCoordinateMin = i - range;
			meta.zCoordinateMax = i + range + 1;
			metaMap.put( (long) i, meta );
			
		}

		
//		EstimateCorrelationsAtSamplePoints.arryImg = ArrayImgs.doubles( 11, 120, nRep );
		

		
		final CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject( zMin + range, zMax - range, range, nData, corrs, metaMap );
		
		final InferFromCorrelationsObject<TranslationModel1D, ScaleModel> inf = new InferFromCorrelationsObject<TranslationModel1D, ScaleModel>(dummyCorrelationsObject, 
				nRep, 
				range, 
				new TranslationModel1D(), 
				new NLinearInterpolatorFactory<DoubleType>(),
				new NLinearInterpolatorFactory<DoubleType>(), 
				new ScaleModel(), 
				1, 
				new OpinionMediatorModel<TranslationModel1D>( new TranslationModel1D() ) );
		
		final ArrayImg<DoubleType, DoubleArray> matrix = inf.correlationsToMatrix( 0l, 0l );
		
		// print matrix
//		for ( int i = 0; i < matrix.dimension( 0 ); ++i ) {
//			for ( final DoubleType h : Views.iterable(Views.hyperSlice(matrix, 0, i) ) ) {
//				System.out.print( h.get()+ ",");
//			}
//			System.out.println();
//		}
		
		
		
		// System.exit(1); 
		
		
		
		final double[] noShiftCoordinates = new double[ initialCoordinates.length ];
		for (int i = 0; i < noShiftCoordinates.length; i++) {
			noShiftCoordinates[i] = i;
		}
		
		final ArrayImg<DoubleType, DoubleArray> coord = inf.estimateZCoordinates( 0, 0, noShiftCoordinates );
		
		new ImageJ();
//		ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.arryImg );
//		ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.matrixImg );
		
//		for ( DoubleType c : coord ) {
//			System.out.println( c.get() );
//		}
		

	}
	
	

}
