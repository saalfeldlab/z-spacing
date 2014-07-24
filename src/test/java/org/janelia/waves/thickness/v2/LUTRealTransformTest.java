package org.janelia.waves.thickness.v2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

public class LUTRealTransformTest
{
	final static private int lutLength = 93;
	static private double[] lut = new double[ lutLength ];

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		final ArrayList< Double > list = new ArrayList();
		final Random rnd = new Random();
		for ( int i = 0; i < lutLength; ++i )
			list.add( ( 2 * rnd.nextDouble() - 1.0 ) * Double.MAX_VALUE );
		Collections.sort( list );
		for ( int i = 0; i < lutLength; ++i )
			lut[ i ] = list.get( i );
	}

	@Test
	public void testfindFloorIndex()
	{
		final LUTRealTransform t = new LUTRealTransform( lut, 3, 3 );
		Method findFloorIndex;
		try
		{
			findFloorIndex = LUTRealTransform.class.getDeclaredMethod( "findFloorIndex", Double.TYPE );
			findFloorIndex.setAccessible( true );
			
			for ( int i = 0; i < lutLength - 1; ++i )
			{
				final double reference = lut[ i ] / 2.0 + lut[ i + 1 ] / 2.0;			
				final Integer index = ( Integer )findFloorIndex.invoke( t, new Double( reference ) );
				assertTrue( index.intValue() == i );
			}
			
			for ( int i = 0; i < lutLength - 1; ++i )
			{
				final double reference = lut[ i ];			
				final Integer index = ( Integer )findFloorIndex.invoke( t, new Double( reference ) );
				assertTrue( index.intValue() == i );
			}
			
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			fail();
		}
	}

}
