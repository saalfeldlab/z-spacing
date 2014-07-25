/**
 * 
 */
package org.janelia.models;

import java.util.ArrayList;
import java.util.Random;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author hanslovskyp
 *
 */
public class ScaleModelTest {
	
	private final long randomSeed = 100;
	private final int numberOfTestSamples = 10000;
	private final double meanSource = 1.0;
	private final double meanTarget = 5.0;
	private final double varianceSource = 0.02;
	private final double varianceTarget = 0.02;
	private final Random rng = new Random( randomSeed );
	
	private float[] source;
	private float[] target;
	private final ArrayList< PointMatch > matches = new ArrayList<PointMatch>();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		
		source = new float[ numberOfTestSamples ];
		target = new float[ numberOfTestSamples ];
		
		for ( int i = 0; i < numberOfTestSamples; ++i ) {
			source[i] = (float) (meanSource + rng.nextGaussian()*varianceSource);
			target[i] = (float) (meanTarget + rng.nextGaussian()*varianceTarget);
			matches.add( new PointMatch( new Point( new float[] { source[i] } ), new Point( new float[] { target[i] } ) ) );
		}
		
	}

	@Test
	public void test() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final ScaleModel model = new ScaleModel();
		model.fit(matches);
		
		Assert.assertEquals( model.apply( new float[] { (float) meanSource } )[0], (float) meanTarget, 0.01f );
	}

}
