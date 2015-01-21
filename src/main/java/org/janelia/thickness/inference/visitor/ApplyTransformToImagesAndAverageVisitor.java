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
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.img.planar.PlanarCursor;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.SingleDimensionLUTRealTransform;

public class ApplyTransformToImagesAndAverageVisitor extends AbstractMultiVisitor {
	
	// must contain exactly one integer format placeholder
	private final String basePath;
	private final ArrayList< RandomAccessibleInterval< FloatType > > images;
	private final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory;
	private ImagePlus targetImg;
	private ImagePlusImg< FloatType, ? > targetImgWrapped;
	private final double scale;
	private double multiplier;
	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	private ArrayImg< FloatType, FloatArray > avgImg;


	public ApplyTransformToImagesAndAverageVisitor(final String basePath, 
			final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, 
			final double scale ) {
		this( new ArrayList<Visitor>(), basePath, new ArrayList< RandomAccessibleInterval< FloatType > >(), interpolatorFactory, scale, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE );
	}
	
	public ApplyTransformToImagesAndAverageVisitor(final String basePath, 
			final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, 
			final double scale,
			final int minX,
			final int minY,
			final int maxX,
			final int maxY) {
		this( new ArrayList<Visitor>(), basePath, new ArrayList< RandomAccessibleInterval< FloatType > >(), interpolatorFactory, scale, minX, minY, maxX, maxY );
	}


	public ApplyTransformToImagesAndAverageVisitor(final ArrayList<Visitor> visitors,
			final String basePath, 
			final ArrayList< RandomAccessibleInterval< FloatType > > images, final InterpolatorFactory< FloatType, RandomAccessible< FloatType > > interpolatorFactory, final double scale,
			final int minX, final int minY, final int maxX, final int maxY ) {
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
		
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		
		this.avgImg = null;
		
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
	
	
	public void average() {
		if ( this.images.size() > 0 ) {
			this.avgImg = ArrayImgs.floats( this.images.get( 0 ).dimension( 0 ), this.images.get( 0 ).dimension( 1 ) );
			for ( final FloatType f : this.avgImg ) {
				f.set( 0.0f );
			}
			for ( int i = 0; i < this.images.size(); ++i ) {
				final Cursor<FloatType> s      = Views.flatIterable( this.images.get( i ) ).cursor();
				final ArrayCursor<FloatType> t = this.avgImg.cursor();
				while( s.hasNext() ) {
					t.next().add( s.next() );
				}
			}
			final float div = 1.0f/this.images.size();
			for ( final FloatType t : this.avgImg ) {
				t.mul( div );
			}
			if ( this.targetImg == null )
				generateTargetImg();
		}
	}
	
	
	private void generateTargetImg() {
		final int minX = this.minX == Integer.MAX_VALUE ? 0 : this.minX;
		final int minY = this.minY == Integer.MAX_VALUE ? 0 : this.minY;
		final int maxX = (int) ( this.maxX == Integer.MAX_VALUE ? this.images.get( 0 ).dimension( 0 ) : this.maxX );
		final int maxY = (int) ( this.maxY == Integer.MAX_VALUE ? this.images.get( 0 ).dimension( 1 ) : this.maxY );
		final FloatProcessor ip = new FloatProcessor( maxX - minX, (int) (( maxY - minY ) * scale) );
		this.targetImg = new ImagePlus( "", ip ); // ImagePlusImgs.floats( max - min, max - min );// ArrayImgs.doubles( max - min, max - min );
		this.targetImgWrapped = ImagePlusImgs.from( this.targetImg );
	}


	@Override
	< T extends RealType< T > > void actSelf( final int iteration, 
			final RandomAccessibleInterval< T > matrix, 
			final double[] lut,
			final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions ) {
		
		if ( this.avgImg == null || this.targetImg == null )
			return;
		
		final ArrayImg<FloatType, ?> tmpImg = this.avgImg.copy();
		if ( positions != null ) {
			for ( int i = 0; i < positions.length; ++i ) {
				if ( i == positions[i] )
					continue;
				final Cursor<FloatType> s = Views.flatIterable( Views.hyperSlice( this.avgImg, 1, positions[i] ) ).cursor();
				final Cursor<FloatType> t = Views.flatIterable( Views.hyperSlice( tmpImg,  1,  i ) ).cursor();
				while ( s.hasNext() )
					t.next().set( s.next() );
			}
		}
		
		final double[] scaledLut = new double[ lut.length ];
		for (int i = 0; i < scaledLut.length; i++) {
			scaledLut[i] = lut[i] * scale;
		}
		final SingleDimensionLUTRealTransform lutTransform = new SingleDimensionLUTRealTransform( scaledLut, 2, 2, 1 );
		
		for ( final FloatType t : this.targetImgWrapped ) {
			t.setReal( 0.0 );
		}
		

		final PlanarCursor<FloatType> targetCursor = ImagePlusAdapter.wrapFloat( targetImg ).cursor();
		final RealRandomAccessible<FloatType> interpolated = Views.interpolate( Views.extendValue( tmpImg, new FloatType( Float.NaN ) ), this.interpolatorFactory );
		final IntervalView<FloatType> transformed = Views.interval( RealViews.transform( interpolated, lutTransform), this.targetImgWrapped );
		final Cursor<FloatType> sourceCursor = Views.flatIterable( transformed ).cursor();
		
		while ( targetCursor.hasNext() ) {
			targetCursor.next().add( sourceCursor.next() );
		}


		IJ.save( targetImg, String.format( this.basePath, iteration ) );
	}

}
