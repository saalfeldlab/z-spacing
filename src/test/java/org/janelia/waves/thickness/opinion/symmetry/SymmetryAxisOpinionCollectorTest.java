package org.janelia.waves.thickness.opinion.symmetry;

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
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.janelia.exception.InconsistencyError;
import org.janelia.waves.thickness.ConstantPair;
import org.janelia.waves.thickness.ConstantTriple;
import org.janelia.waves.thickness.CorrelationsObjectInterface;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;
import org.janelia.waves.thickness.opinion.WeightGenerator;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;
import org.junit.Before;
import org.junit.Test;

public class SymmetryAxisOpinionCollectorTest {

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
		final int range = 1;
		
		final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
		final ArrayList<Double> coordinateShift = new ArrayList<Double>();
		final ArrayList<Double> zShifts         = new ArrayList<Double>();
		
		
		for ( int n = 0; n < nData; ++n ) {
			coordinateBase.add( (double) (n + 1) );
			zShifts.add( 0.0 );
			coordinateShift.add( (double) (n + 1) );
		}
		
		for ( int n = 2; n < nData - 2; ++n ) {
			coordinateBase.set(n, (double) (n + 1) );
			zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
			coordinateShift.set( n, coordinateBase.get(n) + zShifts.get(n) );
		}
		
		
		System.out.println( coordinateBase );
		System.out.println( zShifts );
		
		final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs = 
				new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
		
		for ( int i = 2; i < nData - 2; ++i ) {
			
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

		
		
		
		
		
		
		
		
		
		
		
		CorrelationsObjectInterface dummyCorrelationsObject = new CorrelationsObjectInterface() {
			
			public long getzMin() {
				return (long) zMin;
			}
			
			public long getzMax() {
				return (long) zMax;
			}
			
			public HashMap<Long, Meta> getMetaMap() {
				HashMap<Long, Meta> res = new HashMap<Long, Meta>();
				
				for ( int zBin = 2; zBin < nData - 2; ++zBin ) {
					Meta m = new Meta();
					m.zCoordinateMin = (long) zBin + 1 - range;
					m.zCoordinateMax = (long) zBin + 1 + range + 1; // max is exclusive
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
		};
		
	
		
		double[] weights = new double[nData];
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = 1.0;
		}
		
		WeightGenerator wg = new WeightGenerator() {
			
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
		};
		
		SymmetryAxisOpinionCollector oc = new SymmetryAxisOpinionCollector( new SymmetryAxisOpinionFactory( new BellCurve(), 
					new LevenbergMarquardtOptimizer(), 
					wg ) , 
				dummyCorrelationsObject, coordinateShift, wg, 1, 15, new SymmetryShiftRegularizerKeepZLength(14) );
				
				
				
			
		
		TreeMap<ConstantPair<Long, Long>, ArrayList<Long>> positions = new TreeMap< ConstantPair<Long, Long>, ArrayList<Long> >();
		positions.put( new ConstantPair<Long,Long>( 0l, 0l ), new ArrayList<Long>() );
		
		for ( int i = 2; i < nData - 2; ++ i ) {
		
			
		
			positions.get( new ConstantPair<Long,Long>( 0l, 0l )).add( (long) i + 1 );

		}
		Visitor visitor = new Visitor() {
			
			public void act(TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
					int iteration, double change) {
				// do nothing
			}
		};
		
		boolean doPrint = true;
		
		if ( doPrint ) {
			visitor = new Visitor() {
				
				
				public void act(TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
						int iteration, double change) {
					for ( Entry<ConstantPair<Long, Long>, double[]> entry : coordinates.entrySet() ) {

						System.out.print( iteration + " " + change + "\t[" );
						for ( double e : entry.getValue() ) {
							System.out.print(e + "\t");
						}
						System.out.println("]");
					}
				}
			};
		}
		
		TreeMap< ConstantPair<Long, Long>, double[]> shifted = oc.shiftCoordinates( positions, // xy positions
				1.0,           // shift lambda
				1,             // nCores
				1000,             // nIterations
				0.0000000,   // maximum allowed error
				visitor        // visitor
				);
		
		System.out.println();
	
		for ( Entry<ConstantPair<Long, Long>, double[]> entry : shifted.entrySet() ) {
			System.out.print( entry.getKey() + ": ");
			System.out.println( Arrays.toString( entry.getValue() ) );
		}
		
		
		
		// assertArrayEquals( coordinatesReference, shifted.get( new ConstantPair<Long, Long>( 0l, 0l ) ), 0.001 );
	}

}
