package org.janelia.thickness.inference.visitor;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.imglib2.Cursor;
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
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.thickness.FitWithGradient;
import org.janelia.thickness.LUTRealTransform;
import org.janelia.thickness.SingleDimensionLUTRealTransform;

public class ApplyTransformToImagesAndAverageVisitor extends AbstractMultiVisitor {
	
	// must contain exactly one integer format placeholder
	private final String basePath;
	private final ArrayList< RandomAccessibleInterval< FloatType > > images;
	private final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory;
	private ImagePlus targetImg;
	private ImagePlusImg< FloatType, ? > targetImgWrapped;
	private final double scale;
	private double multiplier;


	public ApplyTransformToImagesAndAverageVisitor(final String basePath, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale ) {
		this( new ArrayList<Visitor>(), basePath, new ArrayList< RandomAccessibleInterval< FloatType > >(), interpolatorFactory, scale );
	}


	public ApplyTransformToImagesAndAverageVisitor(final ArrayList<Visitor> visitors,
			final String basePath, final ArrayList< RandomAccessibleInterval< FloatType > > images, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale ) {
		super(visitors);
		try {
			String.format( basePath, 0 );
		} catch ( final IllegalFormatException e ) {
			e.printStackTrace();
			throw new RuntimeException( "basePath must contain exactly one integer format placeholder, cf String.format" );
		}
		this.basePath = basePath;
		this.images = images;
		this.interpolatorFactory = interpolatorFactory;
		this.scale = scale;
		this.multiplier = images.size();
		
		if ( images.size() > 0 ) {
			
			// check dimensions for consistency
			for ( final RandomAccessibleInterval<FloatType> i : images ) {
				if ( i.numDimensions() != 2 && ! ( i.dimension( 0 ) == images.get( 0 ).dimension( 0 ) && i.dimension( 1 ) == images.get( 0 ).dimension( 1 ) ) ) {
					throw new RuntimeException( "Dimension mismatch!" );
				}
			}
			
			generateTargetImg();
			
		} else {
			
			this.targetImg = null;
			this.targetImgWrapped = null;
			
		}
	}
	
	
	public void addImage( final RandomAccessibleInterval< FloatType > image ) {
		
		if ( this.images.size() != 0 && image.numDimensions() != 2 && ! ( image.dimension( 0 ) == this.targetImg.getWidth() && image.dimension( 1 ) == this.targetImg.getHeight() ) ) {
			throw new RuntimeException( "Dimension mismatch!" );
		} else {
			this.images.add( image );
		}
		
		if ( this.images.size() == 1 ) {
			generateTargetImg();
		}
		
		this.multiplier = this.images.size();
		
	}
	
	
	private void generateTargetImg() {
		final FloatProcessor ip = new FloatProcessor( (int) this.images.get( 0 ).dimension(0), (int) ( this.images.get( 0 ).dimension(1) * scale ) );
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
		final SingleDimensionLUTRealTransform lutTransform = new SingleDimensionLUTRealTransform( scaledLut, 2, 2, 1 );
		
		for ( final FloatType t : this.targetImgWrapped ) {
			t.setReal( 0.0 );
		}
		
		PlanarCursor<FloatType> targetCursor;
		for (  final RandomAccessibleInterval<FloatType> i : this.images ) {
			targetCursor = ImagePlusAdapter.wrapFloat( targetImg ).cursor();
			final RealRandomAccessible<FloatType> interpolated = Views.interpolate( Views.extendValue( i, new FloatType( Float.NaN ) ), this.interpolatorFactory );
			final IntervalView<FloatType> transformed = Views.interval( RealViews.transform( interpolated, lutTransform), this.targetImgWrapped );
			final Cursor<FloatType> sourceCursor = Views.flatIterable( transformed ).cursor();
			
			while ( targetCursor.hasNext() ) {
				targetCursor.next().add( sourceCursor.next() );
			}
		}
		
		targetCursor = ImagePlusAdapter.wrapFloat( targetImg ).cursor();
		while ( targetCursor.hasNext() ) {
			targetCursor.next().mul( this.multiplier );
		}
		

		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
