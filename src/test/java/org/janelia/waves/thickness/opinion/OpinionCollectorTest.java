package org.janelia.waves.thickness.opinion;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.janelia.exception.InconsistencyError;
import org.janelia.waves.thickness.ConstantPair;
import org.janelia.waves.thickness.CorrelationsObjectInterface;
import org.janelia.waves.thickness.functions.FixedMeanOneIntersectBellCurve;
import org.janelia.waves.thickness.functions.FixedMeanOneIntersectBellCurveFactory;
import org.janelia.waves.thickness.functions.OneIntersectBellCurve;
import org.janelia.waves.thickness.functions.OneIntersectBellCurveFactory;
import org.janelia.waves.thickness.opinion.OpinionCollector.Visitor;
import org.junit.Before;
import org.junit.Test;


public class OpinionCollectorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testShiftCoordinatesTreeMapOfLongArrayListOfConstantPairOfLongLongDoubleIntIntDoubleVisitor() throws FunctionEvaluationException, InconsistencyError, InterruptedException {

		Random rng = new Random( 100 );
		
		final int nData = 10;
		
		final int maxVal = 14;
		
		final boolean doPrint = true;
		
		double scaleX = 0.01; // 1;
		double scaleY = 0.00000;
		
		final double[] x1 = new double[nData];
		final double[] x2 = new double[nData];
		
		final double[] y1 = new double[nData];
		final double[] y2 = new double[nData];
		
		final float[] x1f = new float[nData];
		final float[] x2f = new float[nData];
		
		final float[] y1f = new float[nData];
		final float[] y2f = new float[nData];
		
		double sigma1 = 3.0;
		double sigma2 = 3.0;
		
		final double xStart1 = 1.0;
		final double xStart2 = 5.0;
		
		final double xRef1 = 3.0;
		final double xRef2 = 9.0;
		
		final double[] coordinatesReference = new double[maxVal];
		
		final ArrayList<Double> coordinateBase = new ArrayList<Double>();
		
		for (int i = 0; i < maxVal; ++i ) {
			coordinatesReference[i] = i + 1;
		}
		
		for ( int i = 0; i < nData; ++i ) {
			x1[i] = i + xStart1;
			x2[i] = i + xStart2;
			
			x1f[i] = i + (float) xStart1;
			x2f[i] = i + (float) xStart2;
			
			y1[i] = Math.min( 1.0, Math.max( 0.0, new OneIntersectBellCurve( ).value( x1[i], new double[]{ xRef1, sigma1 } ) + scaleY * Math.min( 0.9, Math.max( -0.9, rng.nextGaussian() ) ) ) );
			y2[i] = Math.min( 1.0, Math.max( 0.0, new OneIntersectBellCurve( ).value( x2[i], new double[]{ xRef2, sigma2 } ) + scaleY * Math.min( 0.9, Math.max( -0.9, rng.nextGaussian() ) ) ) );
			
			y1f[i] = (float) y1[i];
			y2f[i] = (float) y2[i];
		}
		
		double[] shifts = new double[ maxVal ];
		
		for ( int i = 0; i < maxVal; ++i ) {
			double prevShift = 0.0;
			if ( i > 0 ) {
				prevShift = shifts[ i - 1 ];
			}
			shifts[i] = scaleX * Math.abs( rng.nextGaussian() )+ prevShift;
		}
		
		for ( int i = 0; i < nData; ++i ) {
			x1[i] += shifts[i];
			x2[i] += shifts[i + 4];
			
			x1f[i] += shifts[i];
			x2f[i] += shifts[i];
		}
		
		for ( double x : x1 ) {
			coordinateBase.add( x );
		}
		
		for ( int i = (int) ( x2.length - ( xStart2 - xStart1 ) ); i < x2.length; ++i ) {
			coordinateBase.add( x2[i] );
		}
		
		System.out.println( coordinateBase );

		System.out.println( Arrays.toString( x1 ) );
		System.out.println( Arrays.toString( x2 ) );
		
		CorrelationsObjectInterface dummyCorrelationsObject = new CorrelationsObjectInterface() {
			
			final ArrayImg<FloatType, FloatArray> cpX1 = generateFloatMember( x1f );
			final ArrayImg<FloatType, FloatArray> cpX2 = generateFloatMember( x2f );
			
			final ArrayImg<FloatType, FloatArray> cpY1 = generateFloatMember( y1f );
			final ArrayImg<FloatType, FloatArray> cpY2 = generateFloatMember( y2f );
			
			final ArrayImg<DoubleType, DoubleArray> cpX1d = generateDoubleMember( x1 );
			final ArrayImg<DoubleType, DoubleArray> cpX2d = generateDoubleMember( x2 );
			
			final ArrayImg<DoubleType, DoubleArray> cpY1d = generateDoubleMember( y1 );
			final ArrayImg<DoubleType, DoubleArray> cpY2d = generateDoubleMember( y2 );
			
			@SuppressWarnings("unused")
			private ArrayImg<FloatType, FloatArray> generateFloatMember( double[] other ) {
				final ArrayImg<FloatType, FloatArray> res = ArrayImgs.floats( other.length );
				ArrayCursor<FloatType> cursor             = res.cursor();
				for ( double o : other ) {
					cursor.next().set( (float) o );
				}
				return res;
			}
			
			private ArrayImg<FloatType, FloatArray> generateFloatMember( float[] other ) {
				final ArrayImg<FloatType, FloatArray> res = ArrayImgs.floats( other.length );
				ArrayCursor<FloatType> cursor             = res.cursor();
				for ( float o : other ) {
					cursor.next().set( o );
				}
				return res;
			}
			
			private ArrayImg<DoubleType, DoubleArray> generateDoubleMember( double[] other ) {
				final ArrayImg<DoubleType, DoubleArray> res = ArrayImgs.doubles( other.length );
				ArrayCursor<DoubleType> cursor             = res.cursor();
				for ( double o : other ) {
					cursor.next().set( o );
				}
				return res;
			}
			
			
			public long getzMin() {
				return (long) xStart1;
			}
			
			public long getzMax() {
				return (long) xStart2 + nData;
			}
			
			public HashMap<Long, Meta> getMetaMap() {
				HashMap<Long, Meta> res = new HashMap<Long, Meta>();
				
				Meta m1 = new Meta();
				Meta m2 = new Meta();
				
				m1.zCoordinateMin = (long) xStart1;
				m1.zCoordinateMax = m1.zCoordinateMin + nData;
				m1.zPosition      = (long) xRef1;
				
				m2.zCoordinateMin = (long) xStart2;
				m2.zCoordinateMax = m2.zCoordinateMin + nData;
				m2.zPosition      = (long) xRef2;
				
				
				res.put( (long) xRef1, m1 );
				res.put( (long) xRef2, m2 );
				
				return res;
			}

			public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> extractCorrelationsAt(
					long x, long y, long z) {
				if ( x != 0  || y != 0 ) {
					return null;
				}
				
				switch( (int) z ) {
				case (int) xRef1:
					return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>( this.cpY1, this.cpX1 );
				case (int) xRef2:
					return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>( this.cpY2, this.cpX2 );
				default:
					return null;
				}
			}


			public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
					long x, long y, long z) {
				if ( x != 0  || y != 0 ) {
					return null;
				}
				
				switch( (int) z ) {
				case (int) xRef1:					
					return new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>( this.cpY1d, this.cpX1d );
				case (int) xRef2:		
					return new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>( this.cpY2d, this.cpX2d );
				default:
					return null;
				}
			}
		};
		
		double[] weights = new double[nData];
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = 1.0;
		}
		
		OpinionCollector oc = new OpinionCollector(new FunctionShiftOpinionFactory(new OneIntersectBellCurveFactory(), 
				new GaussNewtonOptimizer( false ),// new LevenbergMarquardtOptimizer(), 
				new double[] {1.0}, 
				weights), 
				dummyCorrelationsObject, 
				new WeightGenerator() {
					
					public double[] generate(long zMin, long zMax, long zRef) {
						double[] res = new double[ (int) (zMax - zMin)];
						for ( int i = 0; i < res.length; ++i ) {
							res[i] = 1.0;
						}
						return res;
					}

					public double[] generate(double[] coordinates, double zRef) {
						// TODO Auto-generated method stub
						return null;
					}
				},
				coordinateBase,
				new OpinionMediatorWeightedMeanFactory( OpinionMediatorWeightedMean.TYPE.ARITHMETIC ), 
				0.00000000001 );
		
		TreeMap<Long, ArrayList<ConstantPair<Long, Long>>> positions = new TreeMap< Long, ArrayList<ConstantPair<Long, Long>>>();
		
		positions.put( (long) xRef1, new ArrayList<ConstantPair<Long, Long>>());
		// positions.put( (long) xRef2, new ArrayList<ConstantPair<Long, Long>>());
		
		positions.get( (long) xRef1 ).add( new ConstantPair<Long, Long> ( 0l, 0l ) );
		// positions.get( (long) xRef2 ).add( new ConstantPair<Long, Long> ( 0l, 0l ) );
		
		Visitor visitor = new Visitor() {
			
			public void act(TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
					int iteration, double change) {
				// do nothing
			}
		};
		
		
		
		if ( doPrint ) {
			visitor = new Visitor() {
				
				
				public void act(TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
					int iteration, double change) {

					for ( Entry<ConstantPair<Long, Long>, double[]> entry : coordinates.entrySet() ) {

						System.out.print( change + "\t[" );
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
				30,             // nIterations
				0.00000001,   // maximum allowed error
				visitor        // visitor
				);
		
	
		for ( Entry<ConstantPair<Long, Long>, double[]> entry : shifted.entrySet() ) {
			System.out.print( entry.getKey() + ": ");
			System.out.println( Arrays.toString( entry.getValue() ) );
		}
		
		assertArrayEquals( coordinatesReference, shifted.get( new ConstantPair<Long, Long>( 0l, 0l ) ), 0.001 );
	}
}
			


//		
//		
//		
//		fail("Not yet implemented");
//	}
//
//}
