/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

/**
 * @author hanslovskyp
 *
 */
public class ActualCoordinatesTrackerVisitor extends AbstractMultiVisitor {
	
	private final String basePath;
	private final String separator;

	public ActualCoordinatesTrackerVisitor(final String basePath, final String separator ) {
		this( new ArrayList<Visitor>(), basePath, separator );
	}

	public ActualCoordinatesTrackerVisitor( final ArrayList< Visitor > visitors, final String basePath, final String separator ) {
		super( visitors );
		this.basePath = basePath;
		this.separator = separator;
	}

	/* (non-Javadoc)
	 * @see org.janelia.thickness.inference.visitor.AbstractMultiVisitor#actSelf(int, net.imglib2.img.array.ArrayImg, double[], org.janelia.thickness.LUTRealTransform, net.imglib2.img.array.ArrayImg, net.imglib2.img.array.ArrayImg, org.janelia.thickness.FitWithGradient)
	 */
	@Override
	void actSelf(final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix,
			final double[] lut, final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions ) {
		
		final TreeMap<Integer, Integer> tm = new TreeMap< Integer, Integer >();
		
		if ( positions != null ) {
			for ( int i = 0; i < positions.length; ++i ) {
				tm.put( positions[i], i );
			}
		} else {
			for ( int i = 0; i < lut.length; ++i ) {
				tm.put( i, i );
			}
		}
		
		
		final File file = new File( String.format( this.basePath, iteration ) );
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			for ( int r = 0; r < lut.length; ++r ) {
				bw.write( String.format( "%d" + this.separator + "%f" + this.separator + "%f\n", r, lut[r], lut[tm.get(r)] ) );
			}
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
