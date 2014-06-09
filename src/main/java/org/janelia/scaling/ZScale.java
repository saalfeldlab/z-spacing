package org.janelia.scaling;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NativeARGBDoubleType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;



/**
 * ZScale is a class for averaging a stack of images, originally in z-Direction only, i.e. the direction perpendicular
 * to the image plane, when the images are stacked. The actual averaging is left to the {@link Averager} interface,
 * which can also include xy scaling. ZScale acts as a proxy between that interface and the user who has access
 * to lists of files rather than {@link RandomAccessibleInterval}.
 * 
 * For a scaling of a complete stack, the user should write a script (e.g. bash, bsh, jython), which selects
 * the appropriate range of images and then calls ZScale for that range.
 * 
 * Parallelization is left to implementations of the {@link Averager} class.
 * 
 * @author Philipp Hanslovsky <hanslovsky@janelia.hhmi.org>
 *
 * @param <T> Data type of the images.
 */
public class ZScale< T extends NativeType<T> & RealType<T> > {
	
	private final ArrayList<String> filenames;
	private final Averager< T > averager;
	private final T dummy;

	/**
	 * Constructor
	 * @param filenames Compute the average of the image files in filenames.
	 * @param averager A class that implements the {@link Averager} interface
	 */
	public ZScale(final ArrayList<String> filenames, final Averager< T > averager, T dummy ) {
		super();
		this.filenames = filenames;
		this.averager  = averager;
		this.dummy     = dummy;
	}
	
	/**
	 * Translate user input, i.e. a list of files, into an {@link ArrayList} of {@link RandomAccessibleInterval}
	 * as required by {@link Averager}. Also do sanity check on dimensions.
	 * 
	 * @param dummy A dummy variable of type T whose only purpose is determining the class of T.
	 * @return The average as a @{link RandomAccessibleInterval}.
	 */
	public RandomAccessibleInterval< T > average(  ) {
		assert filenames.size() > 0: "Need at least one image for averaging";
		
		ArrayList<RandomAccessibleInterval< T> > images = new ArrayList< RandomAccessibleInterval< T > >();
	
		// Open file with ImageJ and wrap it into a net.imglib2.img.imageplus.ImagePlusImg.
		// Preload first image in stack to determine image dimensions for sanity checks later.
		ImagePlusImg<T, ?> image = wrapCorrectly( IJ.openImage( filenames.get( 0 ) ), dummy );
		images.add( image );
		
		int nDim               = image.numDimensions();
		long[] imageDimensions = new long[nDim]; 
		// long[] newDimensions   = new long[nDim + 1];
		
		for ( int d = 0; d < nDim; ++d ) {
			// newDimensions[d]   = image.dimension(d);
			imageDimensions[d] = image.dimension(d);
		}
		
		// newDimensions[nDim] = filenames.size();
		
		
		// Start from i = 1, since the first image has already been added.
		for ( int i = 1; i < filenames.size(); ++i ) {
			String filename = filenames.get(i);
			// Load and wrap image at filename.
			ImagePlusImg<T, ?> currImage = wrapCorrectly(IJ.openImage( filename ), dummy );
			assert currImage.numDimensions() == imageDimensions.length: "Images must have the same number of dimensions";
			for ( int d = 0; d < nDim; ++d ) {
				assert imageDimensions[d] == currImage.dimension(d): "Image dimensions must agree!";
			}
			// If sanity check passed, add image to list.
			images.add( currImage );
		}
		
		// Average it!
		return this.averager.average( images );
	}
	
	
	/**
	 * Wrap image according to generic type T. Convert the processor first, if neccessary.
	 * The original image may be overwritten, as it will not be used in {@link #average(NativeType)} anymore
	 * after being wrapped. If this class is to be extended for plugin use with ImageJ, {@link #wrapCorrectly(ImagePlus, NativeType)} will
	 * need to be rewritten accordingly, to make sure, that the input will not be overwritten.
	 * 
	 * @param imp Image loaded with ImageJ.
	 * @param dummy Dummy variable for determining the class of generic type T.
	 * @return Wrapped image with the appropriate type.
	 */
	public ImagePlusImg< T, ? > wrapCorrectly( ImagePlus imp, T dummy ) {
		// Convert ImageProcessor according to T.
		ImageProcessor ip = imp.getProcessor();
		if ( dummy instanceof FloatType || dummy instanceof DoubleType ) {
			if ( imp.getType() != ImagePlus.GRAY32 ) {
				imp.setProcessor( ip.convertToFloatProcessor() );
			}
		} else if ( dummy instanceof LongType || dummy instanceof IntType || dummy instanceof UnsignedShortType ) {
			if ( imp.getType() != ImagePlus.GRAY16 ) {
				imp.setProcessor( ip.convertToShortProcessor(true) );
			}
		} else if ( dummy instanceof UnsignedByteType ) {
			if ( imp.getType() != ImagePlus.GRAY8 ) {
				imp.setProcessor( ip.convertToByteProcessor(true) );
			}
		} else if ( dummy instanceof NativeARGBDoubleType ) { 
			if ( imp.getType() != ImagePlus.COLOR_RGB ) {
				imp.setProcessor( ip.convertToRGB() );
			}
		} else {
			throw new RuntimeException("Generic parameter " + dummy.getClass() + " not supported!");
		}
		// Now we can wrap correctly.
		return ImagePlusAdapter.wrap( imp );
	}
	

	/**
	 * Example of use for the ZScale class and also stand-alone averager. Use a bash script for scaling.
	 * This main function still needs a switch for T and - once there are more implementations for {@link Averager} -
	 * a switch for the averaging method.
	 * 
	 * @param args Command line arguments: args[0] - list of files to be averaged, args[1] - output, args[2] - number of cores
	 * @throws IOException
	 */
	public static void main( final String[] args ) throws IOException {
		assert args.length == 5: "Accepts exactly five arguments.";
		if ( args.length != 5 ) throw new RuntimeException( "Must have five arguments!" );
		String fileList  = args[0]; // List of files for averaging.
		String writeName = args[1]; // Where to store the output.
		int nCores       = Integer.parseInt(args[2]); // Use nCores cores for calculations.
		String averaging = args[3];
		long step        = Long.parseLong( args[4] );
		
		
		
		// Read image file names into ArrayList. 
		ArrayList<String> fileNames = new ArrayList<String>();
		FileInputStream fis = new FileInputStream(fileList);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		String line;
		while ( (line = br.readLine() ) != null) {
			fileNames.add(( line ));
		}
		br.close();
		
		// Create ZScale object and average.
		ZScale< FloatType > zScale;
		
		if ( averaging.equalsIgnoreCase( "binaryz" )  ) {
			zScale = new ZScale< FloatType >( fileNames, new BinaryAverager< FloatType >( nCores ), new FloatType(1.0f) );
		} else if ( averaging.equalsIgnoreCase( "binaryxy" ) ) {
			zScale = new ZScale< FloatType >( fileNames, new SingleImageBinaryAverager< FloatType >(step, nCores), new FloatType(1.0f) );
		} else {
			throw new RuntimeException( "Do not understand averaging method " + averaging);
		}
		RandomAccessibleInterval< FloatType > res = zScale.average(  );
		
		// Write result to file.
		ImagePlus writableImage = ImageJFunctions.wrapFloat( res, "TEST" );
		IJ.save( writableImage, writeName );
		
		// Exit with 0 (no errors).
		System.exit(0);
	}
	
}
