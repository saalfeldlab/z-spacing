package org.janelia.waves.thickness.functions;

import static org.junit.Assert.*;

import org.apache.commons.math.FunctionEvaluationException;
import org.junit.Before;
import org.junit.Test;

public class FixedMeanOneIntersectBellCurveTest {
	
	private FixedMeanOneIntersectBellCurve zeroMean = null;
	private FixedMeanOneIntersectBellCurve oneMean  = null;
	
	private double[] p1 = null;
	private double[] p2 = null;

	@Before
	public void setUp() throws Exception {
		zeroMean = new FixedMeanOneIntersectBellCurve( 0.0 );
		oneMean  = new FixedMeanOneIntersectBellCurve( 1.0 );
		
		this.p1 = new double[] { 1.0 };
		this.p2 = new double[] { 2.0 };
	}

	@Test
	public void testValue() throws FunctionEvaluationException {
		
		assertEquals( 1.0, zeroMean.value( 0.0, p1 ), 0.0001 );
		assertEquals( 1.0, zeroMean.value( 0.0, p2 ), 0.0001 );
		assertEquals( Math.exp( - 0.5 ), zeroMean.value( 1.0, p1 ), 0.0001 );
		assertEquals( Math.exp( - 1.0/8.0 ), zeroMean.value( 1.0, p2 ), 0.0001 );
		
		
		assertEquals( zeroMean.value( -1.0, p1), zeroMean.value( 1.0, p1 ), 0.0001 );
		assertEquals( zeroMean.value( -5.0, p2), zeroMean.value( 5.0, p2 ), 0.0001 );
		
		assertEquals( 1.0, oneMean.value( 1.0, p1 ), 0.0001 );
		assertEquals( 1.0, oneMean.value( 1.0, p2 ), 0.0001 );
		assertEquals( Math.exp( - 2.0 ), oneMean.value( - 1.0, p1 ), 0.0001 );
		assertEquals( Math.exp( - 2.0 ), oneMean.value( 5.0, p2 ), 0.0001 );
		
		assertEquals( oneMean.value(  0.0, p1), oneMean.value( 2.0, p1 ), 0.0001 );
		assertEquals( oneMean.value( -5.0, p2), oneMean.value( 7.0, p2 ), 0.0001 );
		
		for ( int i = -10; i <= 10; ++i ) {
			assertEquals( zeroMean.value( (double) i, p1), oneMean.value( (double) ( i + 1 ), p1), 0.0001 );
			assertEquals( zeroMean.value( (double) i, p2), oneMean.value( (double) ( i + 1 ), p2), 0.0001 );
		}
		
	}

	@Test
	public void testGradient() {
	}

	@Test
	public void testDerivative() {
	}

	@Test
	public void testInverse() throws FunctionEvaluationException, DomainError {
		assertArrayEquals( new double[] { 0.0, -0.0 }, zeroMean.inverse( 1.0, p1 ), 0.0001 );
		assertArrayEquals( new double[] { 0.0, -0.0 }, zeroMean.inverse( 1.0, p2 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0, -1.0 }, zeroMean.inverse( Math.exp( - 0.5 ), p1 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0, -1.0 }, zeroMean.inverse( Math.exp( - 1.0/8.0 ), p2 ), 0.0001 );
		
		assertArrayEquals( new double[] { 1.0,  1.0 }, oneMean.inverse( 1.0, p1 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0,  1.0 }, oneMean.inverse( 1.0, p2 ), 0.0001 );
		assertArrayEquals( new double[] { 3.0, -1.0 }, oneMean.inverse( Math.exp( - 2.0 ), p1 ), 0.0001 );
		assertArrayEquals( new double[] { 5.0, -3.0 }, oneMean.inverse( Math.exp( - 2.0 ), p2 ), 0.0001 );
		
	


	}

}
