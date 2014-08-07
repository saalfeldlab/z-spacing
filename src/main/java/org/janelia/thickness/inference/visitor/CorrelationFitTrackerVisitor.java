/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

/**
 * @author hanslovskyp
 *
 */
public class CorrelationFitTrackerVisitor extends AbstractMultiVisitor {
	
	private final String basePath;
	private final int range;
	private final String separator;

	public CorrelationFitTrackerVisitor(final String basePath, final int range, final String separator ) {
		this( new ArrayList<Visitor>(), basePath, range, separator );
	}

	public CorrelationFitTrackerVisitor( final ArrayList< Visitor > visitors, final String basePath, final int range, final String separator ) {
		super( visitors );
		this.basePath = basePath;
		this.range = range;
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
			final double[] estimatedFit ) {
		
		final File file = new File( String.format( this.basePath, iteration ) );
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			for ( final double v : estimatedFit )
				bw.write( String.format( "%f\n", v ) );
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
