package org.janelia.waves.thickness.v2.inference.visitor;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.planar.PlanarRandomAccess;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.waves.thickness.v2.FitWithGradient;
import org.janelia.waves.thickness.v2.LUTRealTransform;

public class CorrelationArrayTrackerVisitor extends AbstractMultiVisitor {
	
	// must contain exactly one integer format placeholder
	private final String basePath;
	private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory;
	private final ImagePlus targetImg;
	private final FinalInterval closedInterval;
	private final int nData;
	private final int range;
	
	private double a1;
	private double a2;
	
	


	public CorrelationArrayTrackerVisitor(final String basePath, final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory, final int nData, final int range ) {
		this( new ArrayList<Visitor>(), basePath, interpolatorFactory, nData, range );
	}


	public CorrelationArrayTrackerVisitor(final ArrayList<Visitor> visitors,
			final String basePath, final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory,
			final int nData, final int range) {
		super(visitors);
		this.basePath = basePath;
		this.interpolatorFactory = interpolatorFactory;
		this.targetImg = new ImagePlus( "", new FloatProcessor( range + 1, 2 * nData) );
		
		try {
			String.format( this.basePath, 0 );
		} catch ( final IllegalFormatException e ) {
			e.printStackTrace();
			throw new RuntimeException( "basePath must contain exactly one integer format placeholder, cf String.format" );
		}
		
		this.closedInterval = new FinalInterval( nData, nData );
		this.nData = nData;
		this.range = range;
	}


	@Override
	void actSelf( final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
			final LUTRealTransform transform,
			final ArrayImg<DoubleType, DoubleArray> multipliers,
			final ArrayImg<DoubleType, DoubleArray> weights,
			final FitWithGradient fitWithGradient) {

		final PlanarRandomAccess<FloatType> targetAccess = ImagePlusAdapter.wrapFloat( targetImg ).randomAccess();
		final RealRandomAccessible<DoubleType> sourceInterpolated = Views.interpolate( Views.extendValue( matrix, new DoubleType( Double.NaN ) ), this.interpolatorFactory);
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> sourceInterpolatedTransformedScaled = RealViews.transformReal(sourceInterpolated, transform);
		final RealRandomAccess<DoubleType> sourceAccess1 = sourceInterpolatedTransformedScaled.realRandomAccess();
		final RealRandomAccess<DoubleType> sourceAccess2 = sourceInterpolatedTransformedScaled.realRandomAccess();
		
		
		for (int n = 0; n < nData; ++n ) {
			
			sourceAccess1.setPosition( n, 0 );
			sourceAccess1.setPosition( n, 1 );
			transform.apply( sourceAccess1, sourceAccess2 );
			sourceAccess1.setPosition( sourceAccess2 );
			
			
			
			for ( int r = 0; r < range; ++r ) {
				
				this.a1 = sourceAccess1.get().get();
				this.a2 = sourceAccess2.get().get();
				
				targetAccess.setPosition( 2*n, 1);
				targetAccess.setPosition( r, 0 );
				targetAccess.get().setReal( a1 );
				
				targetAccess.fwd( 1 );
				targetAccess.get().setReal( a2 );
				
				sourceAccess1.fwd( 0 );
				sourceAccess2.bck( 0 );
			}
			
			
		}

		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
