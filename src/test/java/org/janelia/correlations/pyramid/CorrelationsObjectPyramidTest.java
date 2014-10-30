package org.janelia.correlations.pyramid;

import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.AbstractIntegralCrossCorrelation.CrossCorrelationType;
import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
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
		final IntegralCrossCorrelationFactory<DoubleType, DoubleType, FloatType, DoubleType> iiFactory = new IntegralCrossCorrelationFactory< DoubleType, DoubleType, FloatType, DoubleType >( CrossCorrelationType.SIGNED_SQUARED, new FloatType(), new DoubleType(), new RealDoubleConverter<DoubleType>(), new RealDoubleConverter<DoubleType>() );
		
		final CorrelationsObjectPyramidFactory<DoubleType> factory2 = new CorrelationsObjectPyramidFactory< DoubleType >( images, iiFactory );
		final CorrelationsObjectPyramid pyr2 = factory2.create(range, new long[] {8, 8}, new int[] { 3 }, 0.5, true /*forceCoarsestLevel*/, true /*forceFinestLevel*/ );
		
		final int pos = depth / 2;
		final int x = width / 2;
		final int y = height / 2;
		
		final CorrelationsObjectInterface co = pyr2.get( pyr2.getMaxLevel() );
		final Meta meta = co.getMetaMap().get( pos );
		final Cursor<DoubleType> c = Views.flatIterable( co.extractDoubleCorrelationsAt(x, y, pos).getA() ).cursor();
		
		
	}

}
