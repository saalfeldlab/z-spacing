/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.PermutationTransform;

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
		
		final ArrayImg<DoubleType, DoubleArray> coordinateImage = ArrayImgs.doubles( lut, lut.length );
		final PermutationTransform transform                    = new PermutationTransform( permutation, 1, 1 );
		final IntervalView<DoubleType> permuted                 = Views.interval( new TransformView< DoubleType >( coordinateImage, transform ), coordinateImage );
		
		final File file = new File( String.format( this.basePath, iteration ) );
		try {
			
			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );
			
			int r = 0;
			for ( final Cursor<DoubleType> c = permuted.cursor(); c.hasNext(); ++r ) {
				bw.write( String.format( "%d" + this.separator + "%f" + this.separator + "%f\n", r, lut[r], c.next().get() ) );
			}
			
			bw.close();
		} catch (final IOException e) {
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
