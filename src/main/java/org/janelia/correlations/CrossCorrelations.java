/**
 * 
 */
package org.janelia.correlations;

import ij.ImageJ;

import java.util.Random;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

import org.janelia.utility.transform.MatrixToStrip;
import org.janelia.utility.transform.StripToMatrix;

/**
 * Convenience functions for cross correlations.
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class CrossCorrelations {
	
	
	public static< T extends RealType< T >, U extends RealType< U > & NativeType< U > > ArrayImg< U, ? > toMatrix( 
			final RandomAccessibleInterval< T > input,
			final long[] xy,
			final long range,
			final long[] radius,
			final U type ) {
		final CrossCorrelationFactory<T, T, U> factory = new CrossCorrelationFactory< T, T, U >( type );
		return toMatrix( input, xy, range, radius, factory, type);
	}
	
	
	public static< T extends RealType< T >, U extends RealType< U > & NativeType< U > > ArrayImg< U, ? > toMatrix( 
			final RandomAccessibleInterval< T > input,
			final long[] xy,
			final long range,
			final long[] radius,
			final CrossCorrelationFactoryInterface< T, T, U > factory,
			final U type ) {
		final long zDim  = input.dimension( 2 );
		final long[] dim = new long[] { zDim, zDim };
		
		final U nanDummy = type.copy();
		nanDummy.setReal( Double.NaN );
		final ArrayImg< U, ? > matrix = new ArrayImgFactory< U >().create( dim, nanDummy );
		for ( final U m : matrix )
			m.setReal( Double.NaN );
		
		final ArrayRandomAccess<U> m1 = matrix.randomAccess();
		final ArrayRandomAccess<U> m2 = matrix.randomAccess();
		
		for ( long z1 = 0; z1 < zDim; ++z1 ) {
			m1.setPosition( z1, 0 );
			m1.setPosition( z1, 1 );
			m2.setPosition( z1, 1 );
			m1.get().setReal( 1 );
			for ( long z2 = z1 + 1; z2 < zDim; ++z2 ) {
				final long diff = z2 - z1;
				if ( Math.abs( diff ) > range )
					continue;
				m1.setPosition( z2, 1 );
				m2.setPosition( z2, 0 );
				final RandomAccess<U> cc = factory.create( 
						Views.hyperSlice( input, 2, z1 ), 
						Views.hyperSlice( input, 2, z2 ),
						radius ).randomAccess();
				cc.setPosition( xy );
				final double val = cc.get().getRealDouble();
				m1.get().setReal( val );
				m2.get().setReal( val );
			}
		}
		
		return matrix;
	}
	
	
	public static< T extends RealType< T > > RandomAccessibleInterval< T > toMatrixFromStrip(
			final RandomAccessibleInterval< T > input,
			final int range ) {
		final StripToMatrix t = new StripToMatrix( range );
		final T dummy         = input.randomAccess().get().copy();
		final long zDim       = input.dimension( 1 );
		dummy.setReal( Double.NaN );
		return Views.interval( new TransformView<T>( Views.extendValue( input, dummy ), t ), new FinalInterval( new long[] { zDim, zDim } ));
	}
	
	
	public static< T extends RealType< T >, U extends RealType< U > & NativeType< U > > ArrayImg< U, ? > toStrip( 
			final RandomAccessibleInterval< T > input,
			final long[] xy,
			final long range,
			final long[] radius,
			final CrossCorrelationFactoryInterface< T, T, U > factory,
			final U type ) {
		final long zDim      = input.dimension( 2 );
		final long fullWidth = 2*range + 1;
		final long[] dim     = new long[] { fullWidth, zDim };
		
		final U nanDummy = type.copy();
		nanDummy.setReal( Double.NaN );
		final ArrayImg< U, ? > strip = new ArrayImgFactory< U >().create( dim, nanDummy );
		for ( final U m : strip )
			m.setReal( Double.NaN );
		
		final ArrayCursor<U> s1 = strip.cursor();
		final OutOfBounds<U> s2 = Views.extendValue( strip, nanDummy ).randomAccess();
		
		while( s1.hasNext() ) {
			final U var = s1.next();
			final long dZ = s1.getLongPosition( 0 ) - range;
			final long z1 = s1.getLongPosition( 1 );
			final long z2 = z1 + dZ;
			if ( z2 < 0 || z2 >= zDim )
				var.setReal( Double.NaN );
			else if ( dZ > 0 ) {
				final RandomAccess<U> cc = factory.create( 
						Views.hyperSlice( input, 2, z1 ), 
						Views.hyperSlice( input, 2, z2 ),
						radius ).randomAccess();
				cc.setPosition( xy );
				var.set( cc.get() );
			}
			else if ( dZ < 0 ) {
				System.out.println( z1 + " " + z2 + " " + ( range - dZ ) + " " + (z2 + dZ ) );
				s2.setPosition( z2, 1 );
				s2.setPosition( range - dZ, 0 );
				var.set( s2.get() );
			}
			else
				var.setReal( 1.0 );
		}
		
		return strip;
	}
	
	
	public static< T extends RealType< T > > RandomAccessibleInterval< T > toStripFromMatrix(
			final RandomAccessibleInterval< T > input,
			final int range ) {
		final MatrixToStrip t = new MatrixToStrip( range );
		final T dummy         = input.randomAccess().get().copy();
		final long zDim       = input.dimension( 1 );
		final int fullWidth   = t.getTargetWidth();
		dummy.setReal( Double.NaN );
		return Views.interval( new TransformView<T>( Views.extendValue( input, dummy ), t ), new FinalInterval( new long[] { fullWidth, zDim } ));
	}
	
	
	public static void main(final String[] args) {
		final ArrayImg<DoubleType, DoubleArray> input = ArrayImgs.doubles( 50, 50, 50 );
		final long range = 5;
		final long[] xy = new long[] { 30, 30 };
		final long[] radius = new long[] {10, 10};
		final Random rng = new Random( 10 );
		final CrossCorrelationFactory<DoubleType, DoubleType, DoubleType> factory = new CrossCorrelationFactory<DoubleType, DoubleType, DoubleType>( new DoubleType() );
		final DoubleType type = new DoubleType();
		for ( final DoubleType i : input )
			i.set( rng.nextDouble() );
		final ArrayImg<DoubleType, ?> mat = CrossCorrelations.toMatrix( input, xy, range, radius, factory, type );
		new ImageJ();
		ImageJFunctions.show( mat );
		
		final ArrayImg<DoubleType, ?> str = CrossCorrelations.toStrip( input, xy, range, radius, factory, type);
		ImageJFunctions.show( str );
	    
		final RandomAccessibleInterval<DoubleType> mat2 = toMatrixFromStrip( str, (int)range );
		ImageJFunctions.show( mat2, "transformed mat" );
		
		final RandomAccessibleInterval<DoubleType> str2 = toStripFromMatrix( mat, (int)range );
		ImageJFunctions.show( str2, "transformed str" );
	}

}
