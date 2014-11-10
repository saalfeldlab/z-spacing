/**
 * 
 */
package org.janelia.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class TranslationModelNDTest {
	
	private final int numDimensions = 10;
	private final int numSamples    = 100;
	private final List< PointMatch > matches = new ArrayList< PointMatch >();
	private final float[] result = new float[ numDimensions ];
	private final long seed = 100;
	Random rng = new Random( seed );
	float[] NDIMENSIONAL_ZEROS = new float[ numDimensions ];
	public ExpectedException exception = ExpectedException.none();

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		final double[] tmp = new double[ numDimensions ];
		double weightSum = 0.0;
		for ( int i = 0; i < numSamples; ++i ) {
			final float[] arr = new float[ numDimensions ];
			final float w = rng.nextFloat();
			weightSum += w;
			for ( int j = 0; j < numDimensions; ++j ) {
				final float val = rng.nextFloat();
				arr[j]  = val;
				tmp[j] += w*val; 
			}
			matches.add( new PointMatch( new Point( NDIMENSIONAL_ZEROS ), new Point( arr ), w ) );
		}
		for (int i = 0; i < tmp.length; i++) {
			result[i] = (float) (tmp[i] / weightSum);
		}
	}

	
	@Test
	public void test() {
		final float[] t = new float[ numDimensions ];
		final TranslationModelND model = new TranslationModelND( t );
		try {
			model.fit( matches );
		} catch (final NotEnoughDataPointsException e) {
			Assert.fail( "Did throw exception " + e.getClass().getName() );
		} catch (final IllDefinedDataPointsException e) {
			Assert.fail( "Did throw exception " + e.getClass().getName() );
		}
		Assert.assertArrayEquals( result, t, 0.0f );
		
		
		try {
			model.fit( new ArrayList<PointMatch>() );
			Assert.fail( "Did  not throw " + NotEnoughDataPointsException.class.getName() + " exception " );
		} catch (final NotEnoughDataPointsException e) {
			// in this case, do not fail
			Assert.assertTrue( true );
		} catch (final IllDefinedDataPointsException e) {
			Assert.fail( "Did throw exception " + e.getClass().getName() );
		}
		
	}

}
