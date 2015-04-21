package org.janelia.utility.render;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.Type;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

public class RenderTransformedToInterval {
	
	public static < T extends Type< T > > boolean render( RealRandomAccessible< T > transformed, RandomAccessibleInterval< T > result, int nThreads ) {
		final int nDimensions  = result.numDimensions();
		final int lastDimension = nDimensions - 1;
		final long lastDimensionLength = result.dimension( lastDimension );
		final ArrayList<Callable<Void>> callables = new ArrayList< Callable< Void > >();
		final RandomAccessibleOnRealRandomAccessible<T> rastered = Views.raster( transformed );
		for ( long pos = 0; pos < lastDimensionLength; ++pos ) {
			final Cursor<T> out      = Views.flatIterable( Views.hyperSlice( result, lastDimension, pos ) ).cursor();
			final RandomAccess<T> in = rastered.randomAccess();
			in.setPosition( pos, lastDimension );
			callables.add( new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					while( out.hasNext() ) {
						out.fwd();
						for ( int d = 0; d < lastDimension; ++d )
							in.setPosition( out.getLongPosition( d ), d );
						out.get().set( in.get() );
					}
					return null;
				}
			});
		}
		
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		
		try {
			es.invokeAll( callables );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
