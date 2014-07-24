/**
 * 
 */
package org.janelia.waves.thickness.opinion;

import static org.junit.Assert.assertEquals;

import java.util.TreeMap;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantTriple;
import org.junit.Before;
import org.junit.Test;

/**
 * @author hanslovskyp
 *
 */
public class OpinionMediatorTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
	}

	
	

	/**
	 * Test method for {@link org.janelia.waves.thickness.opinion.OpinionMediatorModel#fit(int)}.
	 * @throws InconsistencyError 
	 * @throws InterruptedException 
	 */
	@Test
	public void testFitInt() throws InconsistencyError, InterruptedException {
		OpinionMediatorModel om        = new OpinionMediatorModel( 0.5 );
		OpinionMediatorWeightedMean sm = new OpinionMediatorWeightedMean();
		
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{  1.0 }, new double[]{ 1.0} );
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{  0.0 }, new double[]{ 1.0} );
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{ -1.0 }, new double[]{ 1.0} );
		
		sm.addOpinions( 1, 2, 3, 4, 3, new double[]{  1.0 }, new double[]{ 1.0} );
		sm.addOpinions( 1, 2, 3, 4, 3, new double[]{  0.0 }, new double[]{ 1.0} );
		sm.addOpinions( 1, 2, 3, 4, 3, new double[]{ -1.0 }, new double[]{ 1.0} );
		
		TreeMap<ConstantTriple<Long, Long, Long>, Double> shiftMap = om.fit( 1 );
		
		assertEquals( 1, shiftMap.size() );
		
		double entry = shiftMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 3l));
		
		double referenceShift = 0.0;
		
		assertEquals( referenceShift, entry, 0.00001f);
		
		
		shiftMap = sm.fit( 1 );
		
		assertEquals( 1, shiftMap.size() );
		
		entry = shiftMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 3l));
		
		referenceShift = 0.0;
		
		assertEquals( referenceShift, entry, 0.00001f);
	}
	
	
	/**
	 * Test method for {@link org.janelia.waves.thickness.opinion.OpinionMediatorModel#fit(int)}.
	 * @throws InconsistencyError 
	 * @throws InterruptedException 
	 */
	@Test
	public void testFitIntAutomatedWeights() throws InconsistencyError, InterruptedException {
		OpinionMediatorModel om        = new OpinionMediatorModel( 0.5 );
		OpinionMediatorWeightedMean sm = new OpinionMediatorWeightedMean();
		
		om.addOpinions( 1, 2, 12, 18, 14, new double[]{  0.0, 0.0, 0.0, 1.0, 0.0, 0.0 } );
		om.addOpinions( 1, 2, 14, 18, 15, new double[]{            0.0, 2.0, 0.0, 0.0 } );
		om.addOpinions( 1, 2, 15, 18, 17, new double[]{                 3.0, 0.0, 0.0 } );
		
		sm.addOpinions( 1, 2, 12, 18, 14, new double[]{  0.0, 0.0, 0.0, 1.0, 0.0, 0.0 } );
		sm.addOpinions( 1, 2, 14, 18, 15, new double[]{            0.0, 2.0, 0.0, 0.0 } );
		sm.addOpinions( 1, 2, 15, 18, 17, new double[]{                 3.0, 0.0, 0.0 } );
		
		TreeMap<ConstantTriple<Long, Long, Long>, Double> shiftMap = om.fit( 1 );
		
		assertEquals( 6, shiftMap.size() );
		
		Double entry = shiftMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 15l));
		
		double referenceShift = 21./11.;
		
		assertEquals( referenceShift, entry, 0.00001f);
		
		shiftMap = sm.fit( 1 );
		
		assertEquals( 6, shiftMap.size() );
		
		entry = shiftMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 15l));
		
		referenceShift = 21./11.;
		
		assertEquals( referenceShift, entry, 0.00001f);
		
		
		
	}
	

}
