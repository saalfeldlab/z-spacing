package org.janelia.thickness.lut;

import java.util.ArrayList;

import net.imglib2.RandomAccess;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransformRandomAccessibleIntervalTest {
	
	private final double[] p1 = new double[] { 1.0, 2.0 };
	private final double[] p2 = new double[] { 3.5, 1.5 };
	private final double[] r1 = new double[ p1.length ];
	private final double[] r2 = new double[ p2.length ];
	
	private final int nShifts = 5;
	
	private final ArrayList<AbstractLUTRealTransform> transforms1 = new ArrayList<AbstractLUTRealTransform>();
	private final ArrayList<AbstractLUTRealTransform> transforms2 = new ArrayList<AbstractLUTRealTransform>();
	private final int[] xyz = new int[] { 3, 4, 5 };
	private final long[] XYZ = new long[] { 3, 4, 5 };
	private final double factor = 1.0 / ( xyz[0] + xyz[1] + xyz[2] );
	
	private final int transform2Dimension = 1;
	
	@Before
	public void setup() {
		int count = 0;
		for ( int z = 0; z < xyz[2]; ++z ) {
			for ( int y = 0; y < xyz[1]; ++y ) {
				for ( int x = 0; x < xyz[0]; ++x ) {
					final double[] shifts = new double[ nShifts ];
					for ( int s = 0; s < nShifts; ++s ) {
						shifts[s] = s + count*factor;
					}
					transforms1.add( new LUTRealTransform( shifts, p1.length, r1.length));
					transforms2.add( new SingleDimensionLUTRealTransform( shifts, p1.length, r1.length, transform2Dimension) );
					++count;
				}
			}
		}
	}


	@Test
	public void test() {
		final TransformRandomAccessibleInterval img1 = new TransformRandomAccessibleInterval( XYZ, transforms1 );
		final TransformRandomAccessibleInterval img2 = new TransformRandomAccessibleInterval( XYZ, transforms2 );
		final RandomAccess<AbstractLUTRealTransform> access1 = img1.randomAccess();
		final RandomAccess<AbstractLUTRealTransform> access2 = img2.randomAccess();
		int count = 0;
		for ( int z = 0; z < xyz[2]; ++z ) {
			for ( int y = 0; y < xyz[1]; ++y ) {
				for ( int x = 0; x < xyz[0]; ++x ) {
					access1.setPosition( new int[] { x, y, z } );
					access2.setPosition( new int[] { x, y, z } );
					final AbstractLUTRealTransform t1 = access1.get();
					final AbstractLUTRealTransform t2 = access2.get();
					
					t1.apply( p1, r1 );
					t1.apply( p2, r2 );
					Assert.assertArrayEquals( new double[] { p1[0] + count*factor, p1[1] + count*factor } , r1, 0.000000001 );
					Assert.assertArrayEquals( new double[] { p2[0] + count*factor, p2[1] + count*factor } , r2, 0.000000001 );
					
					t2.apply( p1, r1 );
					t2.apply( p2, r2 );
					Assert.assertArrayEquals( new double[] { p1[0], p1[1] + count*factor } , r1, 0.000000001 );
					Assert.assertArrayEquals( new double[] { p2[0], p2[1] + count*factor } , r2, 0.000000001 );
					
					++count;
				}
			}
		}
	}

}
