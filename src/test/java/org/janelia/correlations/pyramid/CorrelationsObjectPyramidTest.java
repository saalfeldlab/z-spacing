package org.janelia.correlations.pyramid;

import java.util.Random;

import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.CrossCorrelationType;
import org.janelia.correlations.CrossCorrelationFactory;
import org.janelia.correlations.IntegralCrossCorrelationFactory;
import org.junit.Before;
import org.junit.Test;

public class CorrelationsObjectPyramidTest {

	private final int width  = 200;
	private final int height = 100;
	private final int depth  = 10;
	private final int range  = 8;
	private final ArrayImg< DoubleType, DoubleArray > images = ArrayImgs.doubles( width, height, depth );
	
	private final double sigma = Math.sqrt( 20.0 );
	
	private final Random rng = new Random( 100 );
	
	private double exp( final double x, final double mean, final double sigma ) {
		final double diff = x - mean;
		return Math.exp( - 0.5 * diff * diff / ( sigma * sigma ) );
	}

	@Before
	public void setUp() throws Exception {
		for ( final DoubleType i : images )
			i.set( rng.nextDouble() );
	}

	@Test
	public void test() {
		final CrossCorrelationFactory<DoubleType, DoubleType, FloatType > ccFactory = new CrossCorrelationFactory<DoubleType, DoubleType, FloatType >( new FloatType() );
		final IntegralCrossCorrelationFactory<DoubleType, DoubleType, FloatType, DoubleType> iiFactory = new IntegralCrossCorrelationFactory< DoubleType, DoubleType, FloatType, DoubleType >( CrossCorrelationType.SIGNED_SQUARED, new FloatType(), new DoubleType(), new RealDoubleConverter<DoubleType>(), new RealDoubleConverter<DoubleType>() );
		
//		final long t00 = System.currentTimeMillis();
//		final CorrelationsObjectPyramidFactory<DoubleType> factory = new CorrelationsObjectPyramidFactory< DoubleType >( images, ccFactory );
//		final CorrelationsObjectPyramid pyr = factory.create(range, new long[] {8, 8}, new int[] { 3 }, 0.5, true /*forceCoarsestLevel*/, true /*forceFinestLevel*/ );
//		final long t01 = System.currentTimeMillis();
		
		final long t10 = System.currentTimeMillis();
		final CorrelationsObjectPyramidFactory<DoubleType> factory2 = new CorrelationsObjectPyramidFactory< DoubleType >( images, iiFactory );
		final CorrelationsObjectPyramid pyr2 = factory2.create(range, new long[] {8, 8}, new int[] { 3 }, 0.5, true /*forceCoarsestLevel*/, true /*forceFinestLevel*/ );
		final long t11 = System.currentTimeMillis();
		
//		for ( int i = 0; i < pyr.getNumberOfLevels(); ++i ) {
//			final CorrelationsObjectInterface l = pyr.get( i );
//			final Set<SerializableConstantPair<Long, Long>> cs = l.getXYCoordinates();
//			System.out.println( i );
//			for ( final SerializableConstantPair<Long, Long> c : cs ) 
//				System.out.print( c.getA() + " " + c.getB() + ", " );
//			System.out.println();
//		}
//		
//		System.out.println( ( t01 - t00 ) + " vs. " + ( t11 - t10 ) );
	}

}
