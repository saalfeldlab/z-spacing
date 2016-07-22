/**
 *
 */
package org.janelia.thickness.inference.visitor;

import java.io.IOException;

import org.janelia.thickness.lut.PermutationTransform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class PermutedScalingFactorsVisitor extends CSVVisitor
{

	public PermutedScalingFactorsVisitor( final String basePath, final String relativePattern, final String separator )
	{
		super( basePath, relativePattern, separator );
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] scalingFactors,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{

		final ArrayImg< DoubleType, DoubleArray > coordinateImage = ArrayImgs.doubles( scalingFactors, scalingFactors.length );
		final PermutationTransform transform = new PermutationTransform( permutation, 1, 1 );
		final IntervalView< DoubleType > permuted = Views.interval( new TransformView< DoubleType >( coordinateImage, transform ), coordinateImage );

		try
		{
			final String path = fileDir( iteration );
			createParentDirectory( path );
			write( new IndexedIterable<>( separator, permuted ), path );
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
