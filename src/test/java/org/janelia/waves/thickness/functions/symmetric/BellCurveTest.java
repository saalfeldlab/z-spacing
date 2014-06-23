package org.janelia.waves.thickness.functions.symmetric;

import static org.junit.Assert.*;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.waves.thickness.functions.DomainError;
import org.janelia.waves.thickness.functions.FixedMeanOneIntersectBellCurve;
import org.junit.Before;
import org.junit.Test;

public class BellCurveTest {

	private BellCurve bellCurve;
	
	private double[] p11 = null;
	private double[] p12 = null;
	private double[] p21 = null;
	private double[] p22 = null;
	

	@Before
	public void setUp() throws Exception {
		bellCurve = new BellCurve();
		
		this.p11 = new double[] { 0.0, 1.0 };
		this.p12 = new double[] { 0.0, 2.0 };
		
		this.p21 = new double[] { 1.0, 1.0 };
		this.p22 = new double[] { 1.0, 2.0 };
	}

	@Test
	public void testValue() throws FunctionEvaluationException {
		
		assertEquals( 1.0, bellCurve.value( 0.0, p11 ), 0.0001 );
		assertEquals( 1.0, bellCurve.value( 0.0, p12 ), 0.0001 );
		assertEquals( Math.exp( - 0.5 ), bellCurve.value( 1.0, p11 ), 0.0001 );
		assertEquals( Math.exp( - 1.0/8.0 ), bellCurve.value( 1.0, p12 ), 0.0001 );
		
		
		assertEquals( bellCurve.value( -1.0, p11), bellCurve.value( 1.0, p11 ), 0.0001 );
		assertEquals( bellCurve.value( -5.0, p12), bellCurve.value( 5.0, p12 ), 0.0001 );
		
		assertEquals( 1.0, bellCurve.value( 1.0, p21 ), 0.0001 );
		assertEquals( 1.0, bellCurve.value( 1.0, p22 ), 0.0001 );
		assertEquals( Math.exp( - 2.0 ), bellCurve.value( - 1.0, p21 ), 0.0001 );
		assertEquals( Math.exp( - 2.0 ), bellCurve.value( 5.0, p22 ), 0.0001 );
		
		assertEquals( bellCurve.value(  0.0, p21), bellCurve.value( 2.0, p21 ), 0.0001 );
		assertEquals( bellCurve.value( -5.0, p22), bellCurve.value( 7.0, p22 ), 0.0001 );
		
		for ( int i = -10; i <= 10; ++i ) {
			assertEquals( bellCurve.value( (double) i, p11), bellCurve.value( (double) ( i + 1 ), p21), 0.0001 );
			assertEquals( bellCurve.value( (double) i, p12), bellCurve.value( (double) ( i + 1 ), p22), 0.0001 );
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
		assertArrayEquals( new double[] { 0.0, -0.0 }, bellCurve.inverse( 1.0, p11 ), 0.0001 );
		assertArrayEquals( new double[] { 0.0, -0.0 }, bellCurve.inverse( 1.0, p12 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0, -1.0 }, bellCurve.inverse( Math.exp( - 0.5 ), p11 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0, -1.0 }, bellCurve.inverse( Math.exp( - 1.0/8.0 ), p12 ), 0.0001 );
		
		assertArrayEquals( new double[] { 1.0,  1.0 }, bellCurve.inverse( 1.0, p21 ), 0.0001 );
		assertArrayEquals( new double[] { 1.0,  1.0 }, bellCurve.inverse( 1.0, p22 ), 0.0001 );
		assertArrayEquals( new double[] { 3.0, -1.0 }, bellCurve.inverse( Math.exp( - 2.0 ), p21 ), 0.0001 );
		assertArrayEquals( new double[] { 5.0, -3.0 }, bellCurve.inverse( Math.exp( - 2.0 ), p22 ), 0.0001 );
	

	}
	
	@Test
	public void testAxisOfSymmetry() {
		
		assertEquals( 0.0, bellCurve.axisOfSymmetry( p11 ), 0.0000001 );
		assertEquals( 0.0, bellCurve.axisOfSymmetry( p12 ), 0.0000001 );
		assertEquals( 1.0, bellCurve.axisOfSymmetry( p21 ), 0.0000001 );
		assertEquals( 1.0, bellCurve.axisOfSymmetry( p22 ), 0.0000001 );
		
	}

}
