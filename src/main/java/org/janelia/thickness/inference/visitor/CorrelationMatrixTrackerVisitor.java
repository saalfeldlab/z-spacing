package org.janelia.thickness.inference.visitor;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.planar.PlanarCursor;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

public class CorrelationMatrixTrackerVisitor extends AbstractMultiVisitor {
	
	// must contain exactly one integer format placeholder
	private final String basePath;
	private final int min;
	private final int max;
	private final int maxElement;
	private final long[] minClosedInterval;
	private final long[] maxClosedInterval;
	private final FinalInterval closedInterval;
	private final double scaleFactor;
	private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory;
//	private final ArrayImg<DoubleType, DoubleArray> targetImg;
//	private final ImagePlusImg<FloatType, FloatArray> targetImg;
	private final ImagePlus targetImg;
	private final Scale2D scale;
	
	


	public CorrelationMatrixTrackerVisitor(final String basePath, final int min, final int max, final double scaleFactor, final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory ) {
		this( new ArrayList<Visitor>(), basePath, min, max, scaleFactor, interpolatorFactory );
	}


	public CorrelationMatrixTrackerVisitor(final ArrayList<Visitor> visitors,
			final String basePath, final int min, final int max, final double scaleFactor, final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory ) {
		super(visitors);
		this.basePath = basePath;
		this.min = (int) Math.ceil( scaleFactor * min );
		this.max = (int) Math.ceil( scaleFactor * max );
		this.maxElement = this.max - 1;
		this.minClosedInterval = new long[] { this.min, this.min };
		this.maxClosedInterval = new long[] { this.maxElement, this.maxElement };
		this.closedInterval = new FinalInterval( this.minClosedInterval, this.maxClosedInterval );
		this.scaleFactor = scaleFactor;
		this.interpolatorFactory = interpolatorFactory;
		this.targetImg = new ImagePlus( "", new FloatProcessor( this.max - this.min, this.max - this.min ) );// ImagePlusImgs.floats( max - min, max - min );// ArrayImgs.doubles( max - min, max - min );
		this.scale = new Scale2D( this.scaleFactor, this.scaleFactor );
		
		try {
			String.format( this.basePath, 0 );
		} catch ( final IllegalFormatException e ) {
			e.printStackTrace();
			throw new RuntimeException( "basePath must contain exactly one integer format placeholder, cf String.format" );
		}
		
	}


	@Override
	< T extends RealType< T > > void actSelf( final int iteration, 
			final RandomAccessibleInterval< T > matrix, final double[] lut,
			final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions ) {

		
		ConvertedRandomAccessibleInterval<T, DoubleType> convertedMatrix = new ConvertedRandomAccessibleInterval<T, DoubleType>( matrix, new RealDoubleConverter< T >(), new DoubleType() );
		final PlanarCursor<FloatType> targetCursor = ImagePlusAdapter.wrapFloat( targetImg ).cursor();
		final RealRandomAccessible<DoubleType> sourceInterpolated = Views.interpolate( Views.extendValue( convertedMatrix, new DoubleType( Double.NaN ) ), this.interpolatorFactory);
		final AffineRealRandomAccessible<DoubleType, AffineGet> sourceInterpolatedTransformedScaled = RealViews.affineReal( RealViews.transformReal(sourceInterpolated, transform), scale);
		final Cursor<DoubleType> sourceCursor = Views.flatIterable( Views.interval( Views.raster( sourceInterpolatedTransformedScaled ), this.closedInterval ) ).cursor();
		
		while ( sourceCursor.hasNext() ) {
			targetCursor.next().set( sourceCursor.next().getRealFloat() );
		}

		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
