package org.janelia.utility.realtransform;

import net.imglib2.RealPoint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ScaleAndShiftTest {
	
	private final double sc1 = 1.0, sc2 = 1.5, sc3 = 2.0;
	private final double sh1 = 1.0, sh2 = 1.0, sh3 = 1.3;
	private final double[] scales = new double[] { sc1, sc2, sc3 };
	private final double[] shifts = new double[] { sh1, sh2, sh3 };
	
	private final double[] sourceCoordinate = new double[] { 1.0, 2.0, 3.0 };
	private final double[] targetCoordinate = new double[] { 
			sc1*sourceCoordinate[0] + sh1,
			sc2*sourceCoordinate[1] + sh2,
			sc3*sourceCoordinate[2] + sh3
	};
	
	private final float[] sourceCoordinateFloat = new float[] { 1.0f, 2.0f, 3.0f };
	private final float[] targetCoordinateFloat = new float[] { 
			(float) (sc1*sourceCoordinate[0] + sh1),
			(float) (sc2*sourceCoordinate[1] + sh2),
			(float) (sc3*sourceCoordinate[2] + sh3)
	};
	
	private final RealPoint sourcePoint = new RealPoint( sourceCoordinate.clone() );
	private final RealPoint targetPoint = new RealPoint( targetCoordinate.clone() );
	
	private final double[] result        = new double[ sourceCoordinate.length ];
	private final float[] resultFloat    = new float[ sourceCoordinate.length ];
	private final RealPoint resultPoint   = new RealPoint( result.clone() );
	private final ScaleAndShift transform = new ScaleAndShift( scales, shifts );

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		final ScaleAndShift[] transforms = new ScaleAndShift[] { 
				transform, 
				transform.copy(), 
				transform.inverse().inverse() 
				};

		
		for ( final ScaleAndShift t : transforms ) {
			t.apply( sourceCoordinate, result );
			t.apply( sourceCoordinateFloat, resultFloat );
			t.apply( sourcePoint, resultPoint );
			
			Assert.assertArrayEquals( targetCoordinate, result, 0.0 );
			Assert.assertArrayEquals( targetCoordinateFloat, resultFloat, 0.0f );
			for ( int d = 0; d < resultPoint.numDimensions(); ++d ) {
				Assert.assertEquals( targetPoint.getDoublePosition( d ), resultPoint.getDoublePosition( d ), 0.0 );
			}
		}
	}
	
	@Test
	public void testInverse() {
		
		transform.applyInverse( result, targetCoordinate );
		transform.applyInverse( resultFloat, targetCoordinateFloat );
		transform.applyInverse( resultPoint, targetPoint );
		
		Assert.assertArrayEquals( sourceCoordinate, result, 0.0 );
		Assert.assertArrayEquals( sourceCoordinateFloat, resultFloat, 0.0f );
		for ( int d = 0; d < resultPoint.numDimensions(); ++d ) {
			Assert.assertEquals( sourcePoint.getDoublePosition( d ), resultPoint.getDoublePosition( d ), 0.0 );
		}
		
		transform.inverse().apply( targetCoordinate , result);
		transform.inverse().apply( targetCoordinateFloat, resultFloat );
		transform.inverse().apply( targetPoint, resultPoint );
		
		Assert.assertArrayEquals( sourceCoordinate, result, 0.0 );
		Assert.assertArrayEquals( sourceCoordinateFloat, resultFloat, 0.0f );
		for ( int d = 0; d < resultPoint.numDimensions(); ++d ) {
			Assert.assertEquals( sourcePoint.getDoublePosition( d ), resultPoint.getDoublePosition( d ), 0.0 );
		}
	}

}
