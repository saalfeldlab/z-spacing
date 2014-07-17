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
import org.janelia.waves.thickness.functions.symmetric.AbsoluteLinear;
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
			CorrelationsObjectInterface correlationsObject,
			int nIterations,
			int comparisonRange,
			M correlationFitModel,
			InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> lutInterpolatorFactory,
			InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory,
			L measurementsMultiplierModel,
			int nThreads,
			OpinionMediator shiftMediator ) {
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
		
		Iterator<Long> iterator = this.correlationsObject.getMetaMap().keySet().iterator();
		zMin = iterator.next();
		long zMaxTmp = zMin;
		
		while ( iterator.hasNext() )
			zMaxTmp = iterator.next();
		zMax = zMaxTmp + 1;
	}
	
	public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates( long x, long y, double[] startingCoordinates ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		ArrayImg<DoubleType, DoubleArray> matrix = this.correlationsToMatrix( x, y);
		ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( matrix.dimension( 0 ) );
		
		for ( DoubleType w : weights) {
			w.set( 1.0 );
		}
		
		double[] lut = startingCoordinates;
		
		
		ArrayImg<DoubleType, DoubleArray> coordinates = ArrayImgs.doubles( startingCoordinates.length );
		{ 
			int i = 0; 
			for ( DoubleType c : coordinates ) {
				c.set( startingCoordinates[i]);
			}
		}
		
		LUTRealTransform transform = new LUTRealTransform(lut, this.lutInterpolatorFactory, matrix.numDimensions(), matrix.numDimensions() );
		
		ArrayImg<DoubleType, DoubleArray> mediatedShifts = ArrayImgs.doubles( lut.length );
		
		ArrayCursor<DoubleType> mediatedCursor   = mediatedShifts.cursor();
		ArrayCursor<DoubleType> coordinateCursor = coordinates.cursor();
		
		
		for ( int n = 0; n < this.nIterations; ++n ) {
			double[] vars = new double[ this.comparisonRange ];
			
			EstimateCorrelationsAtSamplePoints.t = n;
			ArrayImg<DoubleType, DoubleArray> estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( matrix, weights, transform, this.comparisonRange, this.correlationFitModel, vars );
			
			
			File f = new File( String.format( "/groups/saalfeld/home/hanslovskyp/fit_iteration=%d.csv", n ) );
			try {
				f.createNewFile();
				FileWriter fw = new FileWriter( f.getAbsoluteFile() );
				BufferedWriter bw = new BufferedWriter( fw );
				ArrayCursor<DoubleType> efCursor = estimatedFit.cursor();
				
				for (int i = 0; i < vars.length; i++) {
					bw.write( String.format( "%d,%f,%f\n", i, efCursor.next().get(), vars[i] ) );
				}
				
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			FitWithGradient fitWithGradient = new FitWithGradient( estimatedFit, new FitWithGradient.SymmetricGradient(), this.fitInterpolatorFactory );
			
			
			ArrayImg<DoubleType, DoubleArray> multipliers = EstimateQualityOfSlice.estimateFromMatrix( matrix, 
					weights, 
					this.measurementsMultiplierModel, 
					coordinates, 
					fitWithGradient.getFit(), 
					this.nThreads);
	
			
			
			
			TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts = ShiftCoordinates.collectShiftsFromMatrix(coordinates, 
					matrix, 
					weights, 
					multipliers, 
					fitWithGradient.getFit(),
					fitWithGradient.getGradient() );
			
			
			
			
			
			this.shiftMediator.mediate( shifts, mediatedShifts );
			
			
			
			mediatedCursor   = mediatedShifts.cursor();
			coordinateCursor = coordinates.cursor();
			
			
			int i = 0;
			
			File file = new File( String.format( "/groups/saalfeld/home/hanslovskyp/shifts_iteration=%d.csv", n ) );
			try {
				file.createNewFile();
				FileWriter fw = new FileWriter( file.getAbsoluteFile() );
				BufferedWriter bw = new BufferedWriter( fw );
				while ( mediatedCursor.hasNext() ) {
					
					coordinateCursor.fwd();
					coordinateCursor.get().setReal( coordinateCursor.get().getRealDouble() - 0.1*mediatedCursor.next().getRealDouble() );
					
					lut[i] -= 0.1*mediatedCursor.next().getRealDouble();
					bw.write( String.format( "%d,%f,%f\n", i, mediatedCursor.get().get(), coordinateCursor.get().get() ) );
					++i;
					
				}
				
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		return coordinates;
	}
	
	
	public ArrayImg< DoubleType, DoubleArray > correlationsToMatrix( long x, long y ) {
		
		int nSlices = this.correlationsObject.getMetaMap().size(); 
		ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( nSlices, nSlices );
		for ( DoubleType m : matrix ) {
			m.set( Double.NaN );
		}
		
		for ( long zRef = this.zMin; zRef < this.zMax; ++zRef ) {
			long relativeZ = zRef - this.zMin;
			RandomAccessibleInterval<DoubleType> correlations = this.correlationsObject.extractDoubleCorrelationsAt( x, y, zRef ).getA();
			IntervalView<DoubleType> row = Views.hyperSlice( matrix, 1, relativeZ);
			
			RandomAccess<DoubleType> correlationsAccess = correlations.randomAccess();
			RandomAccess<DoubleType> rowAccess          = row.randomAccess();
			
			Meta meta = this.correlationsObject.getMetaMap().get( zRef );
			
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
		 
		
		return matrix;
	}
	
	
	public static void main(String[] args) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		
		
		Random rng = new Random( 100 );

		boolean doPrint = false;
		
		final int nData = 200;
		final int zMin  = 1;
		final int zMax  = zMin + nData;
		double xScale   = 0.5;
		double sigma    = 4.0;
		final int range = 10;
		double gradient = -1.0 / range;
		
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
		
		double[] initialCoordinates = new double[ nData - 2*range ];
		
		File file = new File("/groups/saalfeld/home/hanslovskyp/initialcoordinates.csv");
		try {
			file.createNewFile();
			FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			BufferedWriter bw = new BufferedWriter( fw );
		
			for ( int i = 0; i < initialCoordinates.length; ++i ) {
				initialCoordinates[i] = coordinateShift.get( range + i ) - range - 1;
				String writeString = "" + i + "," + initialCoordinates[i] + "\n";
				bw.write( writeString );
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		final TreeMap< Long, Meta > metaMap = new TreeMap<Long, Meta>();
		
		for ( int i = range; i < nData - range; ++i ) {
			
//			System.out.println( i );
			ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> m = measure.cursor();
			for ( int r = - range; r <= range; ++r ) {
//				m.next().set( new BellCurve().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), sigma } ) );
//				m.next().set( new AbsoluteLinear().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), 1.0, gradient } ) );
				m.next().set( Math.abs(coordinateShift.get( i + r ) -  coordinateShift.get( i ) ) * gradient + 1.0 );
//				System.out.println( i + r + " " + m.get().get() );
			}
		
			
			ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> c = coord.cursor();
			for ( int r = - range; r <= range; ++r ) {
				c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
			}

			
			
			corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i) ),
					new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));

			Meta meta = new Meta();
			meta.zPosition = i;
			meta.zCoordinateMin = i - range;
			meta.zCoordinateMax = i + range + 1;
			metaMap.put( (long) i, meta );
			
		}


		
		CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject( zMin + range, zMax - range, range, nData, corrs, metaMap );
		
		InferFromCorrelationsObject<TranslationModel1D, ScaleModel> inf = new InferFromCorrelationsObject<TranslationModel1D, ScaleModel>(dummyCorrelationsObject, 
				25, 
				range, 
				new TranslationModel1D(), 
				new NLinearInterpolatorFactory<DoubleType>(),
				new NLinearInterpolatorFactory<DoubleType>(), 
				new ScaleModel(), 
				1, 
				new OpinionMediatorModel<TranslationModel1D>( new TranslationModel1D() ) );
		
		ArrayImg<DoubleType, DoubleArray> matrix = inf.correlationsToMatrix( 0l, 0l );
		
		for ( int i = 0; i < matrix.dimension( 0 ); ++i ) {
			for ( DoubleType h : Views.iterable(Views.hyperSlice(matrix, 0, i) ) ) {
				System.out.print( h.get()+ ",");
			}
			System.out.println();
		}
		
		new ImageJ();
		ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.arryImg );
		
		// System.exit(1); 
		
		
		
		double[] noShiftCoordinates = new double[ initialCoordinates.length ];
		for (int i = 0; i < noShiftCoordinates.length; i++) {
			noShiftCoordinates[i] = i;
		}
		
		ArrayImg<DoubleType, DoubleArray> coord = inf.estimateZCoordinates( 0, 0, noShiftCoordinates );
		
//		for ( DoubleType c : coord ) {
//			System.out.println( c.get() );
//		}
		

	}
	
	

}
