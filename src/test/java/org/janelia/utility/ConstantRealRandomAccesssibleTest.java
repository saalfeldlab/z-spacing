package org.janelia.utility;

import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.sampler.special.ConstantRealRandomAccessible;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConstantRealRandomAccesssibleTest {
	
	private final DoubleType value = new DoubleType( 1.0 );
	private final int nDim = 5;
	private final ConstantRealRandomAccessible< DoubleType > constantAccessible = new ConstantRealRandomAccessible< DoubleType >( value, nDim );
	private final Random rng = new Random( 100 );
	private final int nRandomIter = 100;
	private final long[] subInterval = new long[ nDim ];

	@Before
	public void setUp() throws Exception {
		for ( int d = 0; d < nDim; ++d ) {
			subInterval[ d ] = 10;
		}
	}

	@Test
	public void test() {
		
		final RealRandomAccess<DoubleType> rra = constantAccessible.realRandomAccess();
		
		for ( int i = 0; i < nRandomIter; ++i ) {
			for ( int d = 0; d < nDim; ++d ) {
				rra.setPosition( rng.nextDouble(), d );
			}
			Assert.assertEquals( value.get(), rra.get().get(), 0.0 );
		}
		
		final IntervalView<DoubleType> iv = Views.interval( Views.raster( constantAccessible ), new FinalInterval( subInterval ) );
		final Cursor<DoubleType> cursor = Views.flatIterable( Views.hyperSlice( iv, 0, subInterval[0]/2) ).cursor();
		
		while ( cursor.hasNext() ) {
			Assert.assertEquals( value.get(), cursor.next().get(), 0.0 );
		}
		
	}

}
