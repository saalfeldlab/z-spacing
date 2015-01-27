/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author hanslovskyp
 *
 */
public class WeightsTrackerVisitor extends AbstractMultiVisitor {
	
	private final String basePath;
	private final String separator;
	
	private int r;

	public WeightsTrackerVisitor(final String basePath, final String separator ) {
		this( new ArrayList<Visitor>(), basePath, separator );
	}

	public WeightsTrackerVisitor( final ArrayList< Visitor > visitors, final String basePath, final String separator ) {
		super( visitors );
		this.basePath = basePath;
		this.separator = separator;
	}

	/* (non-Javadoc)
	 * @see org.janelia.thickness.inference.visitor.AbstractMultiVisitor#actSelf(int, net.imglib2.img.array.ArrayImg, double[], org.janelia.thickness.LUTRealTransform, net.imglib2.img.array.ArrayImg, net.imglib2.img.array.ArrayImg, org.janelia.thickness.FitWithGradient)
	 */
	@Override
	< T extends RealType< T > > void actSelf( 
			final int iteration, 
			final RandomAccessibleInterval< T > matrix, 
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final double[] weights,
			final RandomAccessibleInterval< double[] > estimatedFit
			) {
		
		
		final File file = new File( String.format( this.basePath, iteration ) );
		r = 0;
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			for ( int i = 0; i < weights.length; ++i ) {
				final double w = weights[ inversePermutation[ i ] ];
				bw.write( String.format( "%d" + this.separator + "%f\n", r++, w ) );
			}
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
