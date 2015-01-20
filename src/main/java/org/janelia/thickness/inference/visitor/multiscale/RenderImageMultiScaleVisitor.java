/**
 * 
 */
package org.janelia.thickness.inference.visitor.multiscale;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.SingleDimensionLUTGrid;
import org.janelia.utility.io.IO;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class RenderImageMultiScaleVisitor< T extends RealType<T> & NativeType<T> > implements MultiScaleVisitor {
	
	private final RandomAccessibleInterval< T > image;
	private final InterpolatorFactory< T, RandomAccessible< T > > factory;
	private final String pathFormat;
	
	/**
	 * @param image
	 * @param factory
	 */
	public RenderImageMultiScaleVisitor(final RandomAccessibleInterval<T> image,
			final InterpolatorFactory<T, RandomAccessible<T>> factory,
			final String pathFormat ) {
		super();
		this.image = image;
		this.factory = factory;
		this.pathFormat = pathFormat;
	}
	

	/**
	 * @param image
	 */
	public RenderImageMultiScaleVisitor(final RandomAccessibleInterval<T> image, final String pathFormat ) {
		this( image, new NLinearInterpolatorFactory<T>(), pathFormat );
	}
	
	
	@Override
	public void act(
			final int index,
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii, 
			final int[] steps, 
			final CorrelationsObjectInterface co,
			final Options options) {
		assert lutField.numDimensions() == image.numDimensions();
		assert lutField.numDimensions() == 3;
		new NLinearInterpolatorFactory<T>();
		
		final SingleDimensionLUTGrid transform = new SingleDimensionLUTGrid( 3, 3, lutField, 2 );
		final IntervalView<T> transformed = Views.interval( Views.raster( RealViews.transformReal( Views.interpolate( Views.extendBorder( image ), this.factory ), transform) ), image );
		
		final String path = String.format( pathFormat, index );
		IO.write( transformed, path, "transformed" );
	}

}
