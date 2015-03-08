package org.janelia.utility;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class CopyFromIntervalToInterval {
	
	public static <T extends RealType< T >, U extends IntegerType< U > > void copy( final RandomAccessibleInterval<T> source, final RandomAccessibleInterval<U> target ) {
		assert source.numDimensions() == target.numDimensions(): "Dimension mismatch";
		for ( int d = 0; d < source.numDimensions(); ++ d ) {
			assert target.dimension( d ) == source.dimension( d ): "Dimension mismatch";
		}
		final Cursor<T> sourceCursor = Views.flatIterable( source ).cursor();
		final Cursor<U> targetCursor = Views.flatIterable( target ).cursor();
		
		while ( sourceCursor.hasNext() ) {
			targetCursor.next().setReal( sourceCursor.next().getRealDouble() );
		}
		
//		assert source.numDimensions() == target.numDimensions(): "Dimension mismatch";
//		for ( int d = 0; d < source.numDimensions(); ++ d ) {
//			assert target.dimension( d ) == source.dimension( d ): "Dimension mismatch";
//		}
//		final Cursor<T> sourceCursor = Views.flatIterable( source ).cursor();
//		final Cursor<U> targetCursor = Views.flatIterable( target ).cursor();
//		
//		while ( sourceCursor.hasNext() ) {
//			targetCursor.next().setReal( sourceCursor.next().getRealDouble() );
//		}
	}
	
	public static <T extends RealType< T >, U extends RealType< U > > void copyToRealType( final RandomAccessibleInterval<T> source, final RandomAccessibleInterval<U> target ) {
		assert source.numDimensions() == target.numDimensions(): "Dimension mismatch";
		for ( int d = 0; d < source.numDimensions(); ++ d ) {
			assert target.dimension( d ) == source.dimension( d ): "Dimension mismatch";
		}
		final Cursor<T> sourceCursor = Views.flatIterable( source ).cursor();
		final Cursor<U> targetCursor = Views.flatIterable( target ).cursor();
		
		while ( sourceCursor.hasNext() ) {
			targetCursor.next().setReal( sourceCursor.next().getRealDouble() );
		}
	}
	
	public static < T, U > void copy( final RandomAccessibleInterval<T> source, final RandomAccessibleInterval<U> target, final Converter< T, U > converter ) {
		assert source.numDimensions() == target.numDimensions(): "Dimension mismatch";
		for ( int d = 0; d < source.numDimensions(); ++ d ) {
			assert target.dimension( d ) == source.dimension( d ): "Dimension mismatch";
		}
		final Cursor<T> sourceCursor = Views.flatIterable( source ).cursor();
		final Cursor<U> targetCursor = Views.flatIterable( target ).cursor();
		
		while ( sourceCursor.hasNext() ) {
			final T s = sourceCursor.next();
			final U t = targetCursor.next();
			converter.convert( s, t );
		}
	}
	
	public static <T extends RealType< T >, U extends RealType< U > > void copyToRealTypeIgnoreNaN( final RandomAccessibleInterval<T> source, final RandomAccessibleInterval<U> target ) {
		assert source.numDimensions() == target.numDimensions(): "Dimension mismatch";
		for ( int d = 0; d < source.numDimensions(); ++ d ) {
			assert target.dimension( d ) == source.dimension( d ): "Dimension mismatch";
		}
		final Cursor<T> sourceCursor = Views.flatIterable( source ).cursor();
		final Cursor<U> targetCursor = Views.flatIterable( target ).cursor();
		
		while ( sourceCursor.hasNext() ) {
			final double val = sourceCursor.next().getRealDouble();
			targetCursor.fwd();
			if ( Double.isNaN( val ) )
				continue;
			else
				targetCursor.get().setReal( val );
		}
	}
	
	
}
