package org.janelia.correlations;

import java.util.Map.Entry;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CorrelationsObjectFactoryTest {
	
	private final RandomAccessibleInterval< FloatType > imgs = ArrayImgs.floats( 10, 10, 5 );
	private final int range = 3;

	@Before
	public void setUp() throws Exception {
		
		for ( int z = 0; z < imgs.dimension( 2 ); ++ z ) {
			final Cursor<FloatType> cursor = Views.flatIterable( Views.hyperSlice( imgs, 2, z ) ).cursor();
			
			for ( int i = 1; cursor.hasNext(); ++i ) {
				cursor.next().set( i );
			}
		}
		
	}
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void test() {
		
		final CorrelationsObjectFactory<FloatType> factory = new CorrelationsObjectFactory<FloatType>( imgs );
		final CorrelationsObjectInterface co = factory.create( range, new long[] { 5 } );
		
		final TreeMap<Long, Meta> metaMap = co.getMetaMap();
		for ( final Entry<Long, Meta> entry : metaMap.entrySet() ) {
			final Meta meta = entry.getValue();
			Assert.assertEquals( Math.min( imgs.dimension(2), entry.getKey() + range + 1), meta.zCoordinateMax );
			Assert.assertEquals( Math.max( 0,  entry.getKey() - range ), meta.zCoordinateMin );
			Assert.assertEquals( (long)entry.getKey(), meta.zPosition );
		}
		
		for ( int x = 0; x < imgs.dimension( 0 ); ++x ) {
			for ( int y = 0; y < imgs.dimension( 1 ); ++y ) {
				for ( int z = 0; z < imgs.dimension( 2 ); ++z ) {
					final RandomAccessibleInterval<DoubleType> corrs = co.extractDoubleCorrelationsAt(x, y, z).getA();
					if ( corrs != null ) {
						for ( final DoubleType c : Views.flatIterable( corrs) ) {
							Assert.assertEquals( 1.0, c.get(), 0.0 );
						}
					}
				}
			}
		}
	}

}
