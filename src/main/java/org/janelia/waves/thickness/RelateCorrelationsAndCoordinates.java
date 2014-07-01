package org.janelia.waves.thickness;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.waves.thickness.correlations.CorrelationsObject;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class RelateCorrelationsAndCoordinates {
	public static ConstantTriple<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> createTriple(
			CorrelationsObject co, 
			long x, 
			long y, 
			long z,
			String filename ) throws IOException 
	{
		ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> correlationsAndCoordinates = co.extractCorrelationsAt(x, y, z);
		
		BufferedReader br = new BufferedReader(new FileReader( filename) );
		String line = null;
		
		long start = (long) Views.flatIterable(correlationsAndCoordinates.getB()).firstElement().get();
		for ( int i = 0; i < start; ++i ) {
			br.readLine();
		}
		
		ArrayImg<FloatType, FloatArray> realWorldCoordinates = ArrayImgs.floats( correlationsAndCoordinates.getB().dimension(0) );
		ArrayCursor<FloatType> cursor = realWorldCoordinates.cursor();
		long count = 0;
		while( count < correlationsAndCoordinates.getB().dimension(0) ) {
			line = br.readLine();
			if ( line == null ) {
				br.close();
				throw new RuntimeException( "Inconsistency!");
			}
			cursor.next().set( Long.parseLong( line ) );
			++count;
		}
		
		br.close();
		
		
		return new ConstantTriple<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>(
				correlationsAndCoordinates.getA(), correlationsAndCoordinates.getB(), realWorldCoordinates);
	}
	
	
	public static void writeToFile(
			ConstantTriple<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> triple,
			String filename, boolean with_header, String sep ) throws IOException {
		
		assert ( triple.getA().numDimensions() == triple.getB().numDimensions() ) &&
		       ( triple.getB().numDimensions() == triple.getC().numDimensions() ) &&
		       ( triple.getA().dimension(0) == triple.getB().dimension(0) ) &&
		       ( triple.getB().dimension(0) == triple.getC().dimension(0) ): "Dimension inconsistencies!";
		       
	       FileWriter f0 = new FileWriter(filename);
	       String newline = System.getProperty( "line.separator" );
	       
	       if ( with_header ) {
	    	   f0.write( "corelation" + sep + "z-coordinate" + sep + "real-coordinate" + newline );
	       }
	
	       Cursor<FloatType> cursorA = Views.flatIterable( triple.getA() ).cursor();
	       Cursor<FloatType> cursorB   = Views.flatIterable( triple.getB() ).cursor();
	       Cursor<FloatType> cursorC   = Views.flatIterable( triple.getC() ).cursor();
	       
	       while ( cursorA.hasNext() ) {
	    	   String currentLine = "" + cursorA.next().get() + sep + cursorB.next().get() + sep + cursorC.next().get();
	    	   f0.write( currentLine + newline );
	       }
	       f0.close();
	}
	
	public static void main( String[] args ) {
		
		long N = 10;
		
		ArrayImg<FloatType, FloatArray> corr     = ArrayImgs.floats( N );
		ArrayImg<FloatType, FloatArray> coord    = ArrayImgs.floats( N );
		
		ArrayCursor<FloatType> c1 = corr.cursor();
		ArrayCursor<FloatType> c2   = coord.cursor();
		
		int i = 0;
		while (c1.hasNext()) {
			c1.next().set( 1.0f/(i+1));
			c2.next().set( i );
			++i;
		}
		
		ConstantTriple<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> triple = new ConstantTriple<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>(
				corr, coord, coord);
		
		String filename     = "/groups/saalfeld/home/hanslovskyp/local/tmp/triple_test.dat";
		boolean with_header = true;
		String separator    = ",";
		
		try {
			writeToFile( triple, filename, with_header, separator );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}















































