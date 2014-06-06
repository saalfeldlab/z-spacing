package org.janelia.scaling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.janelia.waves.thickness.ConstantPair;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * This breaks with the typical use of {@link Averager}, in that it does not averages a set of images into a single result image,
 * but averages a single image in xy direction and writes the result into a new image of smaller size. It follows the binary summing
 * strategy of {@link BinaryAverager}.
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> Data type of the {@link RandomAccessibleInterval}.
 */
public class SingleImageBinaryAverager<T extends NativeType<T> & RealType<T>>
		implements Averager<T> {
	
	private final long stepSizeX;
	private final long stepSizeY;
	private final int nCores;
	
	public static final long numDimensions = 2;
	

	public SingleImageBinaryAverager(long stepSize, int nCores) {
		super();
		this.stepSizeX = stepSize;
		this.stepSizeY = stepSize;
		this.nCores = nCores;
	}



	public RandomAccessibleInterval<T> average(
			ArrayList<RandomAccessibleInterval<T>> input)
			throws IllegalArgumentException {
		if ( input.size() != 1 ) {
			throw new IllegalArgumentException( "Processes exactly one image!" );
		}
		
		if ( input.get( 0 ).numDimensions() != SingleImageBinaryAverager.numDimensions ) {
			throw new IllegalArgumentException( "Processes only 2D images!" );
		}

		
		ArrayList<ConstantPair<Long, Long>> xIntervals = generateStepList( (long)0, input.get(0).dimension(0), this.stepSizeX );
		ArrayList<ConstantPair<Long, Long>> yIntervals = generateStepList( (long)0, input.get(0).dimension(1), this.stepSizeY );
		
		long[] dimensions = new long[]{ xIntervals.size(), yIntervals.size() };
		
		ArrayImg<T, ?> result                   = new ArrayImgFactory< T >().create( dimensions, input.get( 0 ).randomAccess().get().copy() );
		ArrayRandomAccess<T> resultRandomAccess = result.randomAccess();
		
		// generate ExecutorService for parallel computation
		ExecutorService executorService = Executors.newFixedThreadPool( this.nCores );
		ArrayList<Callable<Void>> callables = new ArrayList<Callable<Void>>();
				
		for ( int x = 0; x < dimensions[0]; ++x) {
			
			resultRandomAccess.setPosition(x, 0);
			
			Long xMin = xIntervals.get( x ).getA();
			Long xMax = xIntervals.get( x ).getB();
			
			for ( int y = 0; y < dimensions[1]; ++y ) {
				
				// System.out.println( x +"," + y);
				
				resultRandomAccess.setPosition(y, 1);
				
				Long yMin = yIntervals.get( y ).getA();
				Long yMax = yIntervals.get( y ).getB();
				
				final T resultPixel = resultRandomAccess.get();
				
				final IntervalView<T> interval = Views.interval( input.get( 0 ), new long[]{ xMin, yMin }, new long[]{ xMax - 1, yMax - 1 } );
				resultPixel.set( binarySumAverage( interval));
//				callables.add( new Callable<Void>() {
//
//					public Void call() throws Exception {
//						for ( T f : Views.flatIterable(interval) ) {
//							System.out.print(f.getRealFloat() + " ");
//						}
//						System.out.println();
//						resultPixel.set( binarySumAverage( interval ) );
//						return null;
//					}
//					
//				});
			}
		}
		
		// System.out.println( callables.size() );
		
//		try {
//			executorService.invokeAll( callables );
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return result;
	}
	
	public static ArrayList<ConstantPair<Long, Long>> generateStepList( long min, long max, long step ) {
		ArrayList<ConstantPair<Long, Long>> res = new ArrayList<ConstantPair<Long, Long>>();
		long intervalUpper = min;
		while ( min < max ) {
			intervalUpper = Math.min( min + step, max );
			res.add( new ConstantPair<Long, Long>(min, intervalUpper));
			min = intervalUpper;
		}
		return res;
	}
	

	public T binarySumAverage( RandomAccessibleInterval<T> input ) {
		T result = input.randomAccess().copy().get().createVariable();
		result.setZero();

		HashMap<Integer, T> weightedSums = new HashMap< Integer, T >();

		for ( T p : Views.flatIterable( input )) {
			// Initialize w = 1
			T pixel = p.copy();
			int w = 1;

			// Find appropriate pixel to be summed with current pixel, which has weight w
			T hashedValue = weightedSums.remove( w );

			// Loop as long as their are appropriate summands for each weight w
			while ( hashedValue != null ) {

				w *= 2;

				pixel.add( hashedValue );
				pixel.mul( 0.5 );

				hashedValue = weightedSums.remove( w );

			}
			
			weightedSums.put( w, pixel );

		}
		
		ArrayList<Integer> weights = new ArrayList< Integer >( weightedSums.keySet() );
		Collections.sort( weights );
		int s = 0;
		
		for ( final Integer w : weights ) {
			s += w;
			double ratio = ( (double) w ) / s;
			result.mul( 1.0 - ratio );
			
			T tmp = weightedSums.get( w ).copy();
			tmp.mul( ratio );
			
			result.add( tmp );
		}
		return result;
	}

}
