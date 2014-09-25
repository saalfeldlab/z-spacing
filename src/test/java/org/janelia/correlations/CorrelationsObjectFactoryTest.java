package org.janelia.correlations;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.SerializableConstantPair;
import org.janelia.utility.sampler.SparseXYSampler;
import org.janelia.utility.sampler.XYSampler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CorrelationsObjectFactoryTest {
	
	private final RandomAccessibleInterval< FloatType > imgs = ArrayImgs.floats( 10, 10, 5 );
	private final int range = 3;
	private final long[] radius = new long[] { 5 };
	private final ArrayImg< FloatType, FloatArray > randomImage = ArrayImgs.floats( 10, 10, 5 );
	private final Random rng = new Random( 100 );

	@Before
	public void setUp() throws Exception {
		
		for ( int z = 0; z < imgs.dimension( 2 ); ++ z ) {
			final Cursor<FloatType> cursor = Views.flatIterable( Views.hyperSlice( imgs, 2, z ) ).cursor();
			
			for ( int i = 1; cursor.hasNext(); ++i ) {
				cursor.next().set( i );
			}
		}
		
		for ( final FloatType r : randomImage ) {
			r.set( rng.nextFloat() );
		}
		
	}
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testDense() {
		
		final CorrelationsObjectFactory<FloatType> factory = new CorrelationsObjectFactory<FloatType>( imgs );
		final CorrelationsObjectInterface co = factory.create( range, radius );
		
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
	
	
	@Test
	public void testSparse() {
		@SuppressWarnings("unchecked")
		final
		List<SerializableConstantPair<Long, Long>> coords = Arrays.asList( 
				SerializableConstantPair.toPair( 0l, 0l ),
				SerializableConstantPair.toPair( 1l, 2l ),
				SerializableConstantPair.toPair( 3l, 1l )
				);
		final XYSampler sampler = new SparseXYSampler(coords);
		
		final SparseCorrelationsObjectFactory<FloatType> factory = new SparseCorrelationsObjectFactory< FloatType >( imgs, sampler);
		final CorrelationsObjectInterface sco = factory.create( range, radius );
		
		final TreeMap<Long, Meta> metaMap = sco.getMetaMap();
		for ( final Entry<Long, Meta> entry : metaMap.entrySet() ) {
			final Meta meta = entry.getValue();
			Assert.assertEquals( Math.min( imgs.dimension(2), entry.getKey() + range + 1), meta.zCoordinateMax );
			Assert.assertEquals( Math.max( 0,  entry.getKey() - range ), meta.zCoordinateMin );
			Assert.assertEquals( (long)entry.getKey(), meta.zPosition );
		}
		
		for ( final SerializableConstantPair<Long, Long> s : sampler ) {
			for ( int z = 0; z < imgs.dimension( 2 ); ++z ) {
				final ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> corrs = sco.extractDoubleCorrelationsAt( s.getA(), s.getB(), z);
				Assert.assertNotEquals( corrs, null );
				for ( final DoubleType c : Views.flatIterable( corrs.getA() ) )
					Assert.assertEquals( 1.0,  c.get(), 0.0 );
			}
						
		}
		
	}
	
	
	@Test
	public void testEqual() {
		
		final CorrelationsObjectFactory<FloatType> dcof       = new CorrelationsObjectFactory<FloatType>( randomImage );
		final SparseCorrelationsObjectFactory<FloatType> scof = new SparseCorrelationsObjectFactory<FloatType>( randomImage );
		
		final CorrelationsObjectInterface dco = dcof.create( range, radius );
		final CorrelationsObjectInterface sco = scof.create( range, radius );
		
		Assert.assertTrue( dco.equalsMeta( sco ) );
		Assert.assertTrue( dco.equalsXYCoordinates( sco ) );
		Assert.assertTrue( dco.equals( sco ) );
		
	}

}
