package org.janelia.thickness.inference.visitor;

import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.utility.MatrixStripConversion;

import ij.io.FileSaver;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class MatrixVisitor extends FileSaverVisitor
{

	private final int range;

	public MatrixVisitor( final String basePath, final String relativeFilePattern, final int range )
	{
		super( basePath, relativeFilePattern );
		this.range = range;
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFit,
			final double averageShift )
	{
		final T dummy = scaledMatrix.randomAccess().get().createVariable();
		dummy.setReal( Double.NaN );
		final String path = fileDir( iteration );
		if ( iteration == 0 )
			createParentDirectory( path );
		final LUTRealTransform tf = new LUTRealTransform( lut, 2, 2 );

		final RealTransformRealRandomAccessible< T, InverseRealTransform > transformed = RealViews.transformReal( Views.interpolate( Views.extendValue( scaledMatrix, dummy ), new NLinearInterpolatorFactory<>() ), tf );
		final double s = 1.0 / ( lut[ lut.length - 1 ] - lut[ 0 ] ) * lut.length;
		final double o = -lut[ 0 ];
		final ScaleAndTranslation scaleAndTranslation = new ScaleAndTranslation( new double[] { s, s }, new double[] { o, o } );
		final IntervalView< T > offset = Views.interval( Views.raster( RealViews.transformReal( transformed, scaleAndTranslation ) ), scaledMatrix );
		final RandomAccessibleInterval< T > strip = MatrixStripConversion.matrixToStrip( offset, range, dummy );
		new FileSaver( ImageJFunctions.wrap( strip, "" ) ).saveAsTiff( path );
	}

}
