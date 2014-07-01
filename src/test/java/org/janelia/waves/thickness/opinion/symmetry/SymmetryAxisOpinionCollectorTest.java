package org.janelia.waves.thickness.opinion.symmetry;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;
import org.janelia.waves.thickness.functions.symmetric.BellCurveVariableIntersect;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;
import org.janelia.waves.thickness.opinion.weights.DataBasedWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.FitWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.FunctionFitRansacWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.IterativeFitWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.SingleFitWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.TwoFitsWeightGenerator;
import org.janelia.waves.thickness.opinion.weights.WeightGenerator;
import org.junit.Before;
import org.junit.Test;


public class SymmetryAxisOpinionCollectorTest {
	
	private boolean doPrint    = false;
	private boolean showResult = true;
	
	class DummyCorrelationsObject implements CorrelationsObjectInterface {
		
		
		private final long zMin;
		private final long zMax;
		private final int range;
		private final int nData;
		private final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs;
		
		
			
		public DummyCorrelationsObject(
				long zMin,
				long zMax,
				int range,
				int nData,
				TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>> corrs) {
			super();
			this.zMin  = zMin;
			this.zMax  = zMax;
			this.range = range;
			this.nData = nData;
			this.corrs = corrs;
		}

		public long getzMin() {
			return (long) zMin;
		}
		
		public long getzMax() {
			return (long) zMax;
		}
		
		public HashMap<Long, Meta> getMetaMap() {
			HashMap<Long, Meta> res = new HashMap<Long, Meta>();
			
			for ( int zBin = 0; zBin < nData; ++zBin ) {
				Meta m = new Meta();
				m.zCoordinateMin = (long) Math.max( zBin + 1 - range, 1 );
				m.zCoordinateMax = (long) Math.min( zBin + 1 + range + 1, nData + 1 ); // max is exclusive
				m.zPosition      = (long) zBin + 1;
				res.put( (long) zBin + 1, m );
			}
			
		
			return res;
		}

		public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> extractCorrelationsAt(
				long x, long y, long z) {
			return null;
		}


		public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
				long x, long y, long z) {
			return corrs.get( new ConstantTriple<Long, Long, Long>( x, y, z) );
		}
	}
	
	
	class IgnoreIdentityWeightGenerator implements WeightGenerator {
		
		public double[] generate(double[] coordinates, double zRef) {
			double[] res = new double[ coordinates.length ];
			
			for ( int idx = 0; idx < res.length; ++idx ) {
				res[idx] = 1.0;
				if ( Math.abs( zRef - coordinates[idx] ) < 0.001 ) {
					res[idx] = 0.0;
				}
						
			}
			
			return res;
		}
		
		public double[] generate(long zMin, long zMax, long zRef) {
			double[] res = new double[ (int) (zMax - zMin) ];
			
			for ( int idx = 0; idx < res.length; ++idx ) {
				res[idx] = 1.0;
			}
			
			return res;
		}
		
	}

	
	
	class ConstantWeightGenerator implements DataBasedWeightGenerator {

		public void generateAtXY(double[] zAxis,
				CorrelationsObjectInterface correlations, long zBinMin,
				long zBinMax, long zStart, long zEnd, int range, long x, long y) {
			// TODO Auto-generated method stub
			
		}

		public void generate(double[] coordinates, double[] measurements,
				long x, long y, double zRef, long zMin, long zMax,
				long zPosition) {
			// TODO Auto-generated method stub
			
		}

		public double getWeightFor(long x, long y, long zBin) {
			// TODO Auto-generated method stub
			return 1.0;
		}
		
	}
	
	
	class LazyVisitor implements Visitor {
		
		private int iteration;
		private double change;

		public void act(
				TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
				int iteration, double change) {
			this.iteration = iteration;
			this.change    = change;
		}
		
		public String toString() {
			return String.format( "iteration=%d, change=%f", this.iteration, this.change );
		}
		
	}
	
	
	class PrintVisitor implements Visitor {
		
		private int iteration;
		private double change;

		public void act(
				TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
				int iteration, double change) {
			for ( Entry<ConstantPair<Long, Long>, double[]> entry : coordinates.entrySet() ) {

				System.out.print( iteration + " " + change + "\t[" );
				for ( double e : entry.getValue() ) {
					System.out.print(e + "\t");
				}
				System.out.println("]");
				
				this.iteration = iteration;
				this.change    = change;
			}
			
		}
		
		public String toString() {
			return String.format( "iteration=%d, change=%f", this.iteration, this.change );
		}
		
	}
	
	class WriteCoordinatesVisitor implements Visitor {
		
		final private String fileNameBase;

		public WriteCoordinatesVisitor(String fileNameBase) {
			super();
			this.fileNameBase = fileNameBase;
		}

		public void act(
				TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
				int iteration, double change) {
			
			
			for ( Entry<ConstantPair<Long, Long>, double[]> xy : coordinates.entrySet() ) {
				
				
				try {
					Files.write( Paths.get( String.format( this.fileNameBase, iteration ) ), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				String writeString = ""; // String.format( "bin,x=%d_y=%d,\n", xy.getKey().getA(), xy.getKey().getB() );
				
				for ( double c : xy.getValue() ) {
					writeString += "" + c + "\n";
				}
			
				try {
					Files.write( Paths.get( String.format( this.fileNameBase, iteration ) ), writeString.getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit( 1 );
				}
				// TODO Auto-generated method stub
			}
		}
		
	}
	
	
	

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws InconsistencyError, InterruptedException, FunctionEvaluationException {
		Random rng = new Random( 100 );
		
		final int nData = 14;
		final int zMin  = 1;
		final int zMax  = zMin + nData;
		double xScale   = 0.1;
		double sigma    = 4.0;
		final int range = 2;
		
		final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
		final ArrayList<Double> coordinateShift = new ArrayList<Double>();
		final ArrayList<Double> zShifts         = new ArrayList<Double>();
		
		
		for ( int n = 0; n < nData; ++n ) {
			coordinateBase.add( (double) (n + 1) );
			zShifts.add( 0.0 );
			coordinateShift.add( (double) (n + 1) );
		}
		
		for ( int n = range; n < nData - range; ++n ) {
			coordinateBase.set(n, (double) (n + 1) );
			zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
			coordinateShift.set( n, coordinateBase.get(n) + zShifts.get(n) );
		}
		
		
		if ( doPrint  ) {
			System.out.println( coordinateBase );
			System.out.println( zShifts );
			System.out.println( coordinateShift );
		}
		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		for ( int i = range; i < nData - range; ++i ) {
			
			ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> m = measure.cursor();
			for ( int r = - range; r <= range; ++ r ) {
				m.next().set( new BellCurve().value( coordinateBase.get( i + r ), new double[] { coordinateBase.get( i ), sigma } ) );
			}
		
			
			ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> c = coord.cursor();
			for ( int r = - range; r <= range; ++ r ) {
				c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
			}

			
			
			corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i + 1) ),
					new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));
			
		}


		
		CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject(zMin, zMax, range, nData, corrs);
		
	
		
		double[] weights = new double[nData];
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = 1.0;
		}
		
		WeightGenerator wg = new IgnoreIdentityWeightGenerator();
		
		SymmetryAxisOpinionCollector oc = new SymmetryAxisOpinionCollector( new SymmetryAxisOpinionFactory( new BellCurve(), 
					new LevenbergMarquardtOptimizer(), 
					wg ) , 
				dummyCorrelationsObject, coordinateShift, new ConstantWeightGenerator(), zMin, zMax, new SymmetryShiftRegularizerKeepZLength(14) );
				
				
				
			
		
		TreeMap<ConstantPair<Long, Long>, ArrayList<Long>> positions = new TreeMap< ConstantPair<Long, Long>, ArrayList<Long> >();
		positions.put( new ConstantPair<Long,Long>( 0l, 0l ), new ArrayList<Long>() );
		
		for ( int i = range; i < nData - range; ++ i ) {
		
			positions.get( new ConstantPair<Long,Long>( 0l, 0l )).add( (long) i + 1 );

		}
		
		Visitor visitor = new LazyVisitor();
//		Visitor visitor = new WriteCoordinatesVisitor( "/groups/saalfeld/home/hanslovskyp/git/janelia_notes/wave/shift_experience/no_bad_sections/coordinates_%03d.txt" );
		
		
		
		if ( doPrint ) {
			visitor = new PrintVisitor();
		}
		
//		TreeMap< ConstantPair<Long, Long>, double[]> shifted = oc.shiftCoordinates( positions, // xy positions
//				1.0,           // shift lambda
//				1,             // nCores
//				100,             // nIterations
//				0.001,   // maximum allowed change
//				visitor        // visitor
//				);
//		
//		if ( doPrint || showResult ) {
//			System.out.println();
//		
//			for ( Entry<ConstantPair<Long, Long>, double[]> entry : shifted.entrySet() ) {
//				System.out.print( entry.getKey() + ": ");
//				System.out.println( Arrays.toString( entry.getValue() ) );
//			}
//		}
//			
//		
//		double[] coordinatesReference = new double[ coordinateBase.size() ];
//		for ( int i = 0; i < coordinateBase.size(); ++i ) {
//			coordinatesReference[i] = coordinateBase.get( i );
//		}
//		
//		System.out.println( visitor );
//		
//		assertArrayEquals( coordinatesReference, shifted.get( new ConstantPair<Long, Long>( 0l, 0l ) ), 0.01 );
	}
	
	
	
	
	@Test
	public void testBadSections() throws FunctionEvaluationException, InconsistencyError, InterruptedException {
		
		Random rng = new Random( 100 );
		
		double sigma = 4.0;
		
		final int nData = 14;
		final int zMin  = 1;
		final int zMax  = zMin + nData;
		double xScale   = 0.1;
		final int range = 2;
		
		final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
		final ArrayList<Double> coordinateShift = new ArrayList<Double>();
		final ArrayList<Double> zShifts         = new ArrayList<Double>();
		
		
		for ( int n = 0; n < nData; ++n ) {
			coordinateBase.add( (double) (n + 1) );
			zShifts.add( 0.0 );
			coordinateShift.add( (double) (n + 1) );
		}
		
		for ( int n = range; n < nData - range; ++n ) {
			coordinateBase.set(n, (double) (n + 1) );
			zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
			coordinateShift.set( n, coordinateBase.get(n) + zShifts.get(n) );
		}
		
		final ArrayImg< DoubleType, DoubleArray > correlations = ArrayImgs.doubles( nData, nData );
		
		final int[] badSections = new int[] { 8 };
		
		double correlationDiminisher = 0.8;
		
		for ( ArrayCursor<DoubleType> c = correlations.cursor(); c.hasNext();  ) {
			
			c.fwd();
			
			double i = c.getDoublePosition( 0 );
			double j = c.getDoublePosition( 1 );
			
			double ijCorr = new BellCurve().value( i, new double[] { j, sigma } );
			
			if ( i != j ) {
				for (int bs : badSections ) {
					if ( (int) i == bs || (int) j == bs ) {
						ijCorr *= correlationDiminisher;
						break;
					}
				}
			}
			
			c.get().set( ijCorr );
			
			
		}
		
		
		if ( doPrint ) {
		
			for ( int i = 0; i < nData; ++i ) {
				IntervalView<DoubleType> interval = Views.hyperSlice( correlations, 1, i );
				for ( DoubleType j : Views.iterable(interval)) {
					System.out.print( String.format("%.3f", j.get() ) + "," );
				}
				System.out.println("");
			}
			
		}
		

		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		for ( int i = range; i < nData - range; ++i ) {
			
			ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> m = measure.cursor();
			ArrayRandomAccess<DoubleType> ra = correlations.randomAccess();
			for ( int r = - range; r <= range; ++ r ) {
				ra.setPosition( new int[] { i, i + r } );
				m.next().set( ra.get().get() );
			}
		
			
			ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> c = coord.cursor();
			for ( int r = - range; r <= range; ++ r ) {
				c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
			}

			
			
			corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i + 1) ),
					new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));
			
		}
		
		
		CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject(zMin, zMax, range, nData, corrs);
		
WeightGenerator wg = new IgnoreIdentityWeightGenerator();
		
		SymmetryAxisOpinionCollector oc = new SymmetryAxisOpinionCollector( new SymmetryAxisOpinionFactory( new BellCurve(), 
					new LevenbergMarquardtOptimizer(), 
					wg ) , 
				dummyCorrelationsObject, coordinateShift, new ConstantWeightGenerator(), zMin, zMax, new SymmetryShiftRegularizerKeepZLength(14) );
		
//		SymmetryAxisOpinionCollector oc = new SymmetryAxisOpinionCollector( new SymmetryAxisOpinionFactory( new BellCurve(), 
//				new LevenbergMarquardtOptimizer(), 
//				wg ) , 
//			dummyCorrelationsObject, coordinateShift, new SingleFitWeightGenerator( new CurveFitter( new LevenbergMarquardtOptimizer() ), new BellCurve(), new double[] { 1.0, 1.0} ), zMin, zMax, new SymmetryShiftRegularizerKeepZLength(14) );
				
				
				
			
		
		TreeMap<ConstantPair<Long, Long>, ArrayList<Long>> positions = new TreeMap< ConstantPair<Long, Long>, ArrayList<Long> >();
		positions.put( new ConstantPair<Long,Long>( 0l, 0l ), new ArrayList<Long>() );
		
		for ( int i = range; i < nData - range; ++i ) {
		
			positions.get( new ConstantPair<Long,Long>( 0l, 0l )).add( (long) i + 1 );

		}
		
		if ( doPrint ) {
			System.out.println( coordinateBase );
			System.out.println( zShifts );
		}
		
		Visitor visitor = new LazyVisitor();
		
		if ( doPrint ) {
			visitor = new PrintVisitor();
		}
		
//		TreeMap< ConstantPair<Long, Long>, double[]> shifted = oc.shiftCoordinates( positions, // xy positions
//				1.0,           // shift lambda
//				1,             // nCores
//				100,             // nIterations
//				0.0000000,   // maximum allowed error
//				visitor        // visitor
//				);
//		
//		if ( doPrint || showResult ) {
//			System.out.println();
//		
//			for ( Entry<ConstantPair<Long, Long>, double[]> entry : shifted.entrySet() ) {
//				System.out.print( entry.getKey() + ": ");
//				System.out.println( Arrays.toString( entry.getValue() ) );
//			}
//			System.out.println();
//		}
	}
	
	
	
	@Test
	public void testBadSectionsWithWeights() throws FunctionEvaluationException, InconsistencyError, InterruptedException {
		
		Random rng =  new Random( 100 );
		Random rng2 = new Random( 100 );
		
		double sigma = 4.0;
		double noiseSigma = 0.00;
		
		final int nData = 14;
		final int zMin  = 1;
		final int zMax  = zMin + nData;
		double xScale   = 0.1;
		final int range = 2;
		
		final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
		final ArrayList<Double> coordinateShift = new ArrayList<Double>();
		final ArrayList<Double> zShifts         = new ArrayList<Double>();
		
		
		for ( int n = 0; n < nData; ++n ) {
			coordinateBase.add( (double) (n + 1) );
			zShifts.add( 0.0 );
			coordinateShift.add( (double) (n + 1) );
		}
		
		for ( int n = range; n < nData - range; ++n ) {
			coordinateBase.set(n, (double) (n + 1) );
			zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
			coordinateShift.set( n, coordinateBase.get(n) + zShifts.get(n) );
		}
		
		final ArrayImg< DoubleType, DoubleArray > correlations = ArrayImgs.doubles( nData, nData );
		
		final int[] badSections = new int[] { 7 };
		
		double correlationDiminisher = 0.8;
		
		for ( ArrayCursor<DoubleType> c = correlations.cursor(); c.hasNext();  ) {
			
			c.fwd();
			
			double i = c.getDoublePosition( 0 );
			double j = c.getDoublePosition( 1 );
			
			double ijCorr = new BellCurve().value( i, new double[] { j, sigma } ) - noiseSigma * Math.abs( rng2.nextGaussian() );
			
			if ( i != j ) {
				for (int bs : badSections ) {
					if ( (int) i == bs || (int) j == bs ) {
						ijCorr *= correlationDiminisher;
						break;
					}
				}
			}
			
			c.get().set( ijCorr );
			
			
		}
		
		
		if ( doPrint ) {
		
			for ( int i = 0; i < nData; ++i ) {
				IntervalView<DoubleType> interval = Views.hyperSlice( correlations, 1, i );
				for ( DoubleType j : Views.iterable(interval)) {
					System.out.print( String.format("%.3f", j.get() ) + "," );
				}
				System.out.println("");
			}
			
		}
		

		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		for ( int i = 0; i < nData; ++i ) {
			
			ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> m = measure.cursor();
			ArrayRandomAccess<DoubleType> ra = correlations.randomAccess();
			
			
			for ( int r = -range; r <= range; ++r ) {
				if ( i + r < 0 || i + r >= nData ) {
					continue;
				}
				ra.setPosition( new int[] { i, i + r } );
				m.next().set( ra.get().get() );
			}
		
			
			ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
			ArrayCursor<DoubleType> c = coord.cursor();
			for ( int r = - range; r <= range; ++ r ) {
				if ( i + r < 0 || i + r >= nData ) {
					continue;
				}
				c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
			}

			
			
			corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i + 1) ),
					new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));
			
		}
		
		
		CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject(zMin, zMax, range, nData, corrs);
		
		WeightGenerator wg = new IgnoreIdentityWeightGenerator();
		
		ArrayList<Integer> ignoreIndices = new ArrayList<Integer>();
		ignoreIndices.add(  0 );
//		ignoreIndices.add(  1 );
//		ignoreIndices.add( 12 );
		ignoreIndices.add( 13 );
		
		ArrayList<Integer> ignoreForWeightGeneration = new ArrayList<Integer>();
		ignoreForWeightGeneration.add(  0 );
		ignoreForWeightGeneration.add( -1 );
		ignoreForWeightGeneration.add(  1 );
		
		FitWeightGenerator generator = new TwoFitsWeightGenerator( new CurveFitter( new LevenbergMarquardtOptimizer() ), new BellCurve(), new double[] { 1.0, 1.0}, ignoreForWeightGeneration );
		generator = new IterativeFitWeightGenerator( new CurveFitter( new LevenbergMarquardtOptimizer() ), new BellCurve(), new double[] { 1.0, 1.0}, 0.0, 10 );
		generator = new FunctionFitRansacWeightGenerator( new CurveFitter( new LevenbergMarquardtOptimizer() ), new BellCurve(), new double[] { 1.0, 1.0 }, 5, 1 );
		
		SymmetryAxisOpinionCollector oc = new SymmetryAxisOpinionCollector( new SymmetryAxisOpinionFactory( new BellCurve(), 
					new LevenbergMarquardtOptimizer(), 
					wg,
					new double[] {1.0, 1.0} ) , 
				dummyCorrelationsObject, coordinateShift, generator, zMin, zMax, new SymmetryShiftRegularizerNoShiftForPositions(ignoreIndices) );
			
		
		TreeMap<ConstantPair<Long, Long>, ArrayList<Long>> positions = new TreeMap< ConstantPair<Long, Long>, ArrayList<Long> >();
		positions.put( new ConstantPair<Long,Long>( 0l, 0l ), new ArrayList<Long>() );
		
		for ( int i = 0; i < nData; ++ i ) {
		
			positions.get( new ConstantPair<Long,Long>( 0l, 0l ) ).add( (long) i + 1 );

		}
		
		if ( doPrint ) {
			System.out.println( coordinateBase );
			System.out.println( zShifts );
		}
		
		Visitor visitor = new LazyVisitor();
//		visitor = new WriteCoordinatesVisitor( String.format( "/groups/saalfeld/home/hanslovskyp/git/janelia_notes/wave/shift_experience/ransac_noise=%.03f", noiseSigma ) + "/coordinates_%03d.txt" );
		
		if ( doPrint ) {
			visitor = new PrintVisitor();
		}
		
		TreeMap< ConstantPair<Long, Long>, double[]> shifted = oc.shiftCoordinates( positions, // xy positions
				1.0,           // shift lambda
				1,             // nCores
				120,             // nIterations
				0.00001,       // maximum allowed error
				visitor        // visitor
				);
		
		if ( doPrint || showResult ) {
			System.out.println();
		
			for ( Entry<ConstantPair<Long, Long>, double[]> entry : shifted.entrySet() ) {
				System.out.print( entry.getKey() + ": ");
				System.out.println( Arrays.toString( entry.getValue() ) );
			}
			System.out.println();
			System.out.println( visitor );
		}
	}


}
