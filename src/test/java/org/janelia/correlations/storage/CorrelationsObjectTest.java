package org.janelia.correlations.storage;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.correlations.storage.CorrelationsObjectInterface.Meta;
import org.janelia.utility.io.Serialization;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CorrelationsObjectTest {
	
	private final int width  = 4;
	private final int height = 3;
	private final int depth  = 10;
	private final int range  = 8;
	private final ArrayImg< DoubleType, DoubleArray > reference = ArrayImgs.doubles( depth, depth );
	
	private final double sigma = Math.sqrt( 20.0 );
	
	
	private double exp( final double x, final double mean, final double sigma ) {
		final double diff = x - mean;
		return Math.exp( - 0.5 * diff * diff / ( sigma * sigma ) );
	}

	@Before
	public void setUp() throws Exception {
		final ArrayCursor<DoubleType> r = reference.cursor();
		while( r.hasNext() ) {
			r.fwd();
			if ( Math.abs( r.getDoublePosition( 0 ) - r.getDoublePosition( 1 ) ) <= range ) {
				r.get().set( exp( r.getDoublePosition( 0 ), r.getDoublePosition( 1 ), sigma ) );
			} else {
				r.get().set ( Double.NaN );
			}
		}
		
	}
	
	private void testObject( final CorrelationsObjectInterface co ) {
		for ( int x = 0; x < width; ++x ) {
			for ( int y = 0; y < height; ++y ) {
				final ArrayImg<DoubleType, DoubleArray> result = co.toMatrix( x, y );
				for ( int d = 0; d < result.numDimensions(); ++d ) {
					Assert.assertEquals( reference.dimension( d ), result.dimension( d ) );
				}
				final ArrayCursor<DoubleType> res = result.cursor();
				final ArrayCursor<DoubleType> ref = reference.cursor();
				
				while ( res.hasNext() ) {
					ref.fwd();
					res.fwd();
					Assert.assertEquals( ref.get().get(), res.get().get(), 1e-5 );
				}
			}
		}
	}

	@Test
	public void testDense() {
		final CorrelationsObject co = new CorrelationsObject();
		for ( int zRef = 0; zRef < depth; ++zRef ) {
			final int zMin = Math.max( 0, zRef - range );
			final int zMax = Math.min( depth, zRef + range + 1);
			final int interval = zMax - zMin;
			final Meta meta = new Meta();
			meta.zPosition = zRef;
			meta.zCoordinateMin = zMin;
			meta.zCoordinateMax = zMax;
			
			
			final ArrayImg<FloatType, FloatArray> correlations = ArrayImgs.floats( width, height, interval );
			final ArrayCursor<FloatType> c = correlations.cursor();
			while( c.hasNext() ) {
				c.fwd();
				final double pos = c.getDoublePosition( 2 ) + zMin;
				c.get().setReal( exp( pos, zRef, sigma) );
			}
			
			co.addCorrelationImage( zRef, correlations, meta);
		}
		
		testObject( co );

	}
	
	@Test
	public void testSparse() {
		
		final SparseCorrelationsObject sco = new SparseCorrelationsObject();
		
		for ( int x = 0; x < width; ++x ) {
			for ( int y = 0; y < height; ++y ) {
				for ( int zRef = 0; zRef < depth; ++zRef ) {
					final int zMin = Math.max( 0, zRef - range );
					final int zMax = Math.min( depth, zRef + range + 1);
					final int interval = zMax - zMin;
					final double[] correlations = new double[ interval ];
					for (int i = 0; i < correlations.length; i++) {
						correlations[i] = exp( i + zMin, zRef, sigma );
					}
					if ( x == 0 & y == 0 ) {
						final Meta meta = new Meta();
						meta.zPosition = zRef;
						meta.zCoordinateMin = zMin;
						meta.zCoordinateMax = zMax;
						sco.addCorrelationsAt( x, y, zRef, correlations, meta );
					}
					sco.addCorrelationsAt( x, y, zRef, correlations );
				}
			}
		}
		testObject( sco );
		
		final String targetStr = System.getProperty( "user.dir" ) + "/sco.sr";
		SparseCorrelationsObject deserialized = null;
		deserialized = new SparseCorrelationsObject();
		Serialization.serializeGeneric( sco, targetStr );
		deserialized = Serialization.deserializeGeneric( targetStr, deserialized );
		
		Assert.assertEquals( sco.tolerance, deserialized.tolerance, 0.0f );
		Assert.assertTrue( sco.equals( deserialized ) );
	}

}
