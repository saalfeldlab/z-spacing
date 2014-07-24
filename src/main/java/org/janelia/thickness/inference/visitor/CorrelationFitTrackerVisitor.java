/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.RealRandomAccess;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.FitWithGradient;
import org.janelia.thickness.LUTRealTransform;

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
			final double[] lut, final LUTRealTransform transform,
			final ArrayImg<DoubleType, DoubleArray> multipliers,
			final ArrayImg<DoubleType, DoubleArray> weights,
			final FitWithGradient fitWithGradient) {
		
		final RealRandomAccess<DoubleType> f = fitWithGradient.getFit().realRandomAccess();
		final RealRandomAccess<DoubleType> g = fitWithGradient.getGradient().realRandomAccess();
		
		final File file = new File( String.format( this.basePath, iteration ) );
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			for ( int r = 0; r < range; ++r ) {
				f.setPosition( r, 0 );
				g.setPosition( r, 0 );
				bw.write( String.format( "%d" + this.separator + "%f" + this.separator + "%f\n", r, f.get().getRealDouble(), g.get().getRealDouble() ) );
			}
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
