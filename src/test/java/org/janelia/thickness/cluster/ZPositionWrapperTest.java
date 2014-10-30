/**
 * 
 */
package org.janelia.thickness.cluster;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ZPositionWrapperTest {
	
	Random rng = new Random( 100 );
	int nSamples = 5000;
	double[] samples = new double[ nSamples ];
	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		for ( int i = 0; i < nSamples; ++i ) {
			samples[i] = rng.nextDouble();
		}
	}

	@Test
	public void test() {
		final List<ZPositionWrapper> list = ZPositionWrapper.toList( samples );
		Assert.assertEquals( samples.length, list.size() );
		for ( int i = 0; i < samples.length; ++i ) {
			final double[] arr = list.get(i).getPoint();
			Assert.assertEquals( 1, arr.length );
			Assert.assertEquals( samples[i], arr[0], 0.0 );
		}
	}
	
	@Test
	public void testExceptionConstructor() {
		try {
			new ZPositionWrapper( new double[] { 1, 2, 3 } );
		} catch (final RuntimeException e) {
			final String m = e.getMessage();
			Assert.assertEquals( "ZPositionWrapper: zPosition must have length 1!", m );
		}
		
		exception.expect( RuntimeException.class );
		new ZPositionWrapper( new double[] { 1, 2 } );
	}

}
