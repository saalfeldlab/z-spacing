package org.janelia.thickness.inference.visitor;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import org.janelia.thickness.lut.SingleDimensionLUTRealTransform;
import org.janelia.utility.CopyFromIntervalToInterval;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ApplyTransformToImageVisitor extends AbstractMultiVisitor
{

	// must contain exactly one integer format placeholder
	private final String basePath;

	private final RandomAccessibleInterval< FloatType > image;

	private final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory;

	private final ImagePlus targetImg;

	private final ImagePlusImg< FloatType, ? > targetImgWrapped;

	private final double scale;

	public ApplyTransformToImageVisitor( final String basePath, final RandomAccessibleInterval< FloatType > image, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale )
	{
		this( new ArrayList< Visitor >(), basePath, image, interpolatorFactory, scale );
	}

	public ApplyTransformToImageVisitor( final ArrayList< Visitor > visitors,
			final String basePath, final RandomAccessibleInterval< FloatType > image, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale )
	{
		super( visitors );
		try
		{
			String.format( basePath, 0 );
		}
		catch ( final IllegalFormatException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "basePath must contain exactly one integer format placeholder, cf String.format" );
		}
		this.basePath = basePath;
		this.image = image;
		this.interpolatorFactory = interpolatorFactory;
		this.scale = scale;

		final FloatProcessor ip = new FloatProcessor( ( int ) image.dimension( 0 ), ( int ) ( image.dimension( 1 ) * scale ) );
		this.targetImg = new ImagePlus( "", ip ); // ImagePlusImgs.floats( max -
													// min, max - min );//
													// ArrayImgs.doubles( max -
													// min, max - min );
		this.targetImgWrapped = ImagePlusImgs.from( this.targetImg );

	}

	@Override
	< T extends RealType< T > > void actSelf(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{

		final double[] scaledLut = new double[ lut.length ];
		for ( int i = 0; i < scaledLut.length; i++ )
		{
			scaledLut[ i ] = lut[ i ] * scale;
		}
		final SingleDimensionLUTRealTransform lutTransform = new SingleDimensionLUTRealTransform( scaledLut, 2, 2, 1 );

		final RealRandomAccessible< FloatType > interpolated = Views.interpolate( Views.extendValue( this.image, new FloatType( Float.NaN ) ), this.interpolatorFactory );
		// TODO permute lut and image first!
		final IntervalView< FloatType > transformed = Views.interval( RealViews.transform( interpolated, lutTransform ), this.targetImgWrapped );
		Views.flatIterable( transformed ).cursor();

		CopyFromIntervalToInterval.copyToRealType( transformed, this.targetImgWrapped );

		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
