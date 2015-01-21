package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

public class PositionTrackerVisitor extends AbstractMultiVisitor {
	
	private final String basePath;
	private final String separator;
	
	private int r;

	public PositionTrackerVisitor( final String basePath, final String separator ) {
		this( new ArrayList<Visitor>(), basePath, separator );
	}
	
	public PositionTrackerVisitor( final ArrayList<Visitor> al, final String basePath, final String separator ) {
		super( al );
		this.basePath  = basePath;
		this.separator = separator; 
	}

	@Override
	< T extends RealType< T > > void actSelf( final int iteration, 
			final RandomAccessibleInterval< T > matrix, final double[] lut,
			final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions ) {
		
		if ( positions == null )
			return;
		
		final TreeMap<Integer, Integer> tm = new TreeMap< Integer, Integer>();
		
		for ( int i = 0; i < positions.length; ++i ) {
			tm.put( positions[i], i );
		}
		
		final File file = new File( String.format( this.basePath, iteration ) );
		r = 0;
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			for ( final int c : positions ) {
				bw.write( String.format( "%d" + this.separator + "%d" + this.separator +"%d\n", r++, c, (int)tm.get( c ) ) );
			}
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
