package org.janelia.scaling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This class implements the {@link Averager} interface. Instead of simply adding each
 * entry of the list of {@link RandomAccessibleInterval} to the result, this class builds
 * a binary tree of weighted pairwise sums, in order to keep the partial summands at approximately
 * the same range of values and thus avoiding floating point precision issues.
 * @author hanslovskyp <hanslovskyp@janelia.hhmi.org>
 *
 * @param <T> Data type of the {@link RandomAccessibleInterval}.
 */
public class BinaryAverager<T extends NativeType<T> & RealType< T> > implements Averager<T> {
	

	private final int nCores;
	
	/**
	 * Default constructor - Use all available processors.
	 */
	public BinaryAverager() {
		this( Runtime.getRuntime().availableProcessors() );
	}

	/**
	 * Constructor
	 * @param nCores Number of cores used for summing up. For each sum, both summands are
	 * split into nCores chunks of equal size and each core sums up one chunk. 
	 */
	public BinaryAverager(int nCores) {
		super();
		this.nCores = nCores;
	}

	
	/**
	 * Calculate pairwise sums for all {@link RandomAccessibleInterval} in @param input (without
	 * replacement). Then do the same for the resulting pairwise sums. Continue until only
	 * one summand is left. In general, the size of input is no power of 2, thus the weights have
	 * to be stored for a potential "asymmetric" sum. This method avoids numeric floating points
	 * issues by keeping the values approximately the same, given that the entries in input
	 * are approximately the same.
	 * @param input {@link ArrayList} of {@link RandomAccessibleInterval}
	 */
	public RandomAccessibleInterval<T> average(
			ArrayList<RandomAccessibleInterval<T>> input) {
		
		// generate ExecutorService for parallel computation
		ExecutorService executorService = Executors.newFixedThreadPool( this.nCores );
		
		// Determine number of pixels in RandomAccessibleInterval.
		// This is important for determining the chunk sizes.
		int numDimensions = input.get(0).numDimensions();
		long[] dimensions = new long[numDimensions];
		long numPixels    = 1;
		for ( int d = 0; d < numDimensions; ++d ) {
			dimensions[d] = input.get( 0 ).dimension( d );
			numPixels    *= dimensions[0];
		}
		
		// Generate list of flatIterables, which can easily be divided into chunks.
		final ArrayList<IterableInterval<T>> flatIterables = new ArrayList< IterableInterval<T> >();
		for ( RandomAccessibleInterval<T> image : input ) {
			flatIterables.add( Views.flatIterable( image ) );
		}
		
		// initialize result image to 0
		ArrayImg<T, ?> result = new ArrayImgFactory< T >().create( dimensions, input.get( 0 ).randomAccess().get() );
		final IterableInterval<T> flatResult = Views.flatIterable( result );
		// FIXME Avoid this loop by finding a way of how to initialize the image to 0.
		for ( T pix : flatResult ) {
			pix.setZero();
		}
		
		// Get size of the chunks (step). If numPixels is not a power of 2, then
		// the last chunk will be larger than the others.
		long step = numPixels / (long) this.nCores;
		
		// Store the weights (key) and the according intermediate sum (value) in a hash map.
		// For each image in input, start with a weight w = 1 and see if w is present in
		// weightedSums.keySet(). If so, calculate the mean of the image and  weightedSums.get(w)
		// and multiply w by 2 while removing ( w, weightedSums.get(w) ) from the map. Repeat
		// this step, until w is not present in weightedSums.keySet() anymore and then continue
		// with the next entry in input.
		HashMap<Integer, IterableInterval<T>> weightedSums = new HashMap< Integer, IterableInterval< T > >();
		
		
		for ( final IterableInterval<T> iterableImage : flatIterables ) {
			
			// Initialize w = 1
			int w = 1;
			
			// Find appropriate image to be summed with current image, which has weight w
			IterableInterval<T> hashedImage = weightedSums.remove( w );
			
			// Loop as long as their are appropriate summands for each weight w
			while ( hashedImage != null ) {
				
				// Double w for each time we "climb up" in the binary tree.
				w *= 2;
				
				// Create final variable to grant access in Callable later.
				final IterableInterval<T> finalHashedImage = hashedImage;
				
				// Create list of Callables for parallel execution. Each Callable will
				// be responsible for one chunk.
				ArrayList<Callable<Void>> callables = new ArrayList<Callable<Void>>();
				// Split up each image in nCores subparts (chunks) and average this subparts in parallel.
				for ( int c = 0; c < this.nCores; ++c ) {
					
					// The cth junk holds pixels c * step to ( c + 1 ) * step - 1, with respect
					// to the flatIterable of the original image.
					final long indexStart = c * step;
					long indexEnd         = ( c + 1 ) * step;
					// For last Callable, add remaining pixels that would have been left out otherwise.
					if ( c == this.nCores - 1 ) {
						indexEnd          = numPixels > indexEnd ? numPixels : indexEnd;
					}
					// The current step is not equal to step only for the last Callable					
					final long currStep   = indexEnd - indexStart;
					
					callables.add( new Callable<Void>() {
						public Void call() {
							// Convenience, less typing.
							Cursor<T> cursor       = iterableImage.cursor();
							Cursor<T> hashedCursor = finalHashedImage.cursor();
							
							// Set cursors to beginning of chunk.
							cursor.jumpFwd( indexStart );
							hashedCursor.jumpFwd( indexStart );
							
							// Average two chunks and write result into cursor.
							for ( long count = 0; count < currStep; ++count ) {
								double val = 0.5 * ( cursor.next().getRealDouble() + hashedCursor.next().getRealDouble() );
								cursor.get().setReal( val );
							}
							return null;
							
						}
						
					});
				}
				try {
					// invokeAll starts all Callables in collection callables and waits until all of them have finished.
					executorService.invokeAll(callables);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Delete entry for weight w. The value for w has been combined with another images and "moves up"
				// in the binary tree.
				hashedImage = weightedSums.remove( w );
			}
			
			// If no appropriate sums are present anymore (i.e. no entry for weight w), add 
			// the image to the map at key w.
			weightedSums.put( w, iterableImage );
		}
		
		// If the size of input is not a power of 2, the number of entries in weightedSums is not 1.
		// The remaining entries must be summed up, weighted by the respective key (weight w), normalized
		// by the sum of all weights s.
		ArrayList<Integer> weights = new ArrayList< Integer >( weightedSums.keySet() );
		Collections.sort( weights );
		int s = 0;
		
		for ( final Integer w : weights ) {
			s += w;
			
			// Generate callables (cf above).
			ArrayList<Callable<Void>> callables = new ArrayList<Callable<Void>>();
			for ( int c = 0; c < this.nCores; ++c ) {
				
				// Indexes and step are generated as above.
				final long indexStart = c * step;
				long indexEnd         = ( c + 1 ) * step;
				if ( c == this.nCores - 1 ) {
					indexEnd          = numPixels > indexEnd ? numPixels : indexEnd;
				}
				final long currStep   = indexEnd - indexStart;
				
				// Get cursors and set them to the beginning of the chunk.
				final Cursor<T> hashedCursor = weightedSums.get( w ).cursor();
				final Cursor<T> resultCursor = flatResult.cursor();
				
				hashedCursor.jumpFwd( indexStart );
				resultCursor.jumpFwd( indexStart );
				
				// Need final variable for use in Callable.
				final int currWeightSum = s;
				
				callables.add( new Callable<Void>() {
					public Void call() {
						
						for ( long count = 0; count < currStep; ++count ) {
							double ratio = ( (double) w ) / currWeightSum;
							// curr_sum  = old_val * ( 1 - w/current_s ) + new_val * w/current_s
							double val   = resultCursor.next().getRealDouble() * ( 1.0 - ratio ) +
									       hashedCursor.next().getRealDouble() * ratio;
							resultCursor.get().setReal( val );
						}
						return null;

						
					}
					
				});
			}
			try {
				// Run all Callables and wait for termination.
				executorService.invokeAll(callables);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

}
