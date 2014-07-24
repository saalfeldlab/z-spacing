package org.janelia.waves.thickness.v2.inference.visitor;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.img.planar.PlanarCursor;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.utility.CopyFromIntervalToInterval;
import org.janelia.waves.thickness.v2.FitWithGradient;
import org.janelia.waves.thickness.v2.LUTRealTransform;
import org.janelia.waves.thickness.v2.SingleDimensionLUTRealTransform;

public class ApplyTransformToImageVisitor extends AbstractMultiVisitor {
	
	// must contain exactly one integer format placeholder
	private final String basePath;
	private final RandomAccessibleInterval< FloatType > image;
	private final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory;
	private final ImagePlus targetImg;
	private final ImagePlusImg< FloatType, ? > targetImgWrapped;
	private final double scale;
	private final SingleDimensionLUTRealTransform lutTransform;
	


	public ApplyTransformToImageVisitor(final String basePath, final RandomAccessibleInterval< FloatType > image, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale ) {
		this( new ArrayList<Visitor>(), basePath, image, interpolatorFactory, scale );
	}


	public ApplyTransformToImageVisitor(final ArrayList<Visitor> visitors,
			final String basePath, final RandomAccessibleInterval< FloatType > image, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale ) {
		super(visitors);
		try {
			String.format( basePath, 0 );
		} catch ( final IllegalFormatException e ) {
			e.printStackTrace();
			throw new RuntimeException( "basePath must contain exactly one integer format placeholder, cf String.format" );
		}
		this.basePath = basePath;
		this.image = image;
		this.interpolatorFactory = interpolatorFactory;
		this.scale = scale;
		this.lutTransform = new SingleDimensionLUTRealTransform( new double[ (int) image.dimension( 1 ) ], new NLinearInterpolatorFactory<DoubleType>(), 2, 2, 1 );
		
		final FloatProcessor ip = new FloatProcessor( (int) image.dimension(0), (int) ( image.dimension(1) * scale ) );
		this.targetImg = new ImagePlus( "", ip ); // ImagePlusImgs.floats( max - min, max - min );// ArrayImgs.doubles( max - min, max - min );
		this.targetImgWrapped = ImagePlusImgs.from( this.targetImg );
		
	}


	@Override
	void actSelf( final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
			final LUTRealTransform transform,
			final ArrayImg<DoubleType, DoubleArray> multipliers,
			final ArrayImg<DoubleType, DoubleArray> weights,
			final FitWithGradient fitWithGradient) {
		
		final double[] scaledLut = new double[ lut.length ];
		for (int i = 0; i < scaledLut.length; i++) {
			scaledLut[i] = lut[i] * scale;
		}
		lutTransform.update( scaledLut );

		final PlanarCursor<FloatType> targetCursor = ImagePlusAdapter.wrapFloat( targetImg ).cursor();
		final RealRandomAccessible<FloatType> interpolated = Views.interpolate( Views.extendValue( this.image, new FloatType( Float.NaN ) ), this.interpolatorFactory );
		final IntervalView<FloatType> transformed = Views.interval( RealViews.transform( interpolated, this.lutTransform), this.targetImgWrapped );
		Views.flatIterable( transformed ).cursor();
		
		
		CopyFromIntervalToInterval.copyToRealType( transformed, this.targetImgWrapped );


		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
