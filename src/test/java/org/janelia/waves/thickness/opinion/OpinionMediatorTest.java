/**
 * 
 */
package org.janelia.waves.thickness.opinion;

import static org.junit.Assert.*;

import java.util.TreeMap;

import mpicbg.models.Tile;
import mpicbg.models.TranslationModel1D;

import org.janelia.exception.InconsistencyError;
import org.janelia.waves.thickness.ConstantTriple;
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
	 * Test method for {@link org.janelia.waves.thickness.opinion.OpinionMediator#fit(int)}.
	 * @throws InconsistencyError 
	 * @throws InterruptedException 
	 */
	@Test
	public void testFitInt() throws InconsistencyError, InterruptedException {
		OpinionMediator om = new OpinionMediator( 0.5 );
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{  1.0 }, new double[]{ 1.0} );
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{  0.0 }, new double[]{ 1.0} );
		om.addOpinions( 1, 2, 3, 4, 3, new double[]{ -1.0 }, new double[]{ 1.0} );
		
		TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> tileMap = om.fit( 1 );
		
		assertEquals( 1, tileMap.size() );
		
		Tile<TranslationModel1D> entry = tileMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 3l));
		
		float[] referenceShift = new float[]{ -0.0f};
		
		assertArrayEquals( referenceShift, entry.getModel().apply( new float[] {0.0f}), 0.00001f);
		
	}
	
	
	/**
	 * Test method for {@link org.janelia.waves.thickness.opinion.OpinionMediator#fit(int)}.
	 * @throws InconsistencyError 
	 * @throws InterruptedException 
	 */
	@Test
	public void testFitIntAutomatedWeights() throws InconsistencyError, InterruptedException {
		OpinionMediator om = new OpinionMediator( 0.5 );
		om.addOpinions( 1, 2, 12, 18, 14, new double[]{  0.0, 0.0, 0.0, 1.0, 0.0, 0.0 } );
		om.addOpinions( 1, 2, 14, 18, 15, new double[]{  0.0, 2.0, 0.0, 0.0 } );
		om.addOpinions( 1, 2, 15, 18, 17, new double[]{ 3.0, 0.0, 0.0 } );
		
		TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> tileMap = om.fit( 1 );
		
		assertEquals( 6, tileMap.size() );
		
		Tile<TranslationModel1D> entry = tileMap.get( new ConstantTriple<Long, Long, Long>(1l, 2l, 15l));
		
		float[] referenceShift = new float[]{ 21.f/11.f };
		
		assertArrayEquals( referenceShift, entry.getModel().apply( new float[] {0.0f}), 0.00001f);
		
		
		
	}
	

}
