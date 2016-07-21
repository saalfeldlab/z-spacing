/**
 * 
 */
package org.janelia.utility.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class IO
{

	public static < T extends RealType< T > & NativeType< T > > void write( final RandomAccessibleInterval< T > image, final String filename )
	{
		final String title = "";
		write( image, filename, title );
	}

	public static < T extends RealType< T > & NativeType< T > > void write( final RandomAccessibleInterval< T > image, final String filename, final String title )
	{
		createDirectoryForFile( filename );
		final ImagePlus wrapped = ImageJFunctions.wrap( image, title );
		IJ.save( wrapped.duplicate(), filename );

	}

	public static void write( final String data, final String filename ) throws FileNotFoundException
	{
		createDirectoryForFile( filename );
		final PrintWriter outFile = new PrintWriter( filename );
		outFile.println( data );
		outFile.close();
	}

	public static void createDirectoryForFile( final String filename )
	{
		final int lastSeparatorIndex = filename.lastIndexOf( File.separator );
		final String dirPath = filename.substring( 0, lastSeparatorIndex );
		final File f = new File( dirPath );
		f.mkdirs();
	}

}
