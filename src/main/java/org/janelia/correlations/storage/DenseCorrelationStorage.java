/**
 * 
 */
package org.janelia.correlations.storage;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CrossCorrelationFactoryInterface;
import org.janelia.correlations.FloatingPointIntegralCrossCorrelationFactory;
import org.janelia.correlations.AbstractIntegralCrossCorrelation.CrossCorrelationType;
import org.janelia.utility.ConstantRealRandomAccesssible;
import org.janelia.utility.CopyFromIntervalToInterval;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class DenseCorrelationStorage< T extends RealType< T > & NativeType< T > > implements CorrelationStorageInterface<T>,
		RandomAccessibleInterval<T> {
	
	
	
	/**
	 * @param store
	 */
	public DenseCorrelationStorage( final ArrayImg<T, ?> store ) {
		super();
		this.store = store;
	}

	private final ArrayImg< T, ? > store;
	
	public static < U extends RealType< U >, V extends RealType< V > & NativeType< V > >DenseCorrelationStorage< V > create (
			final RandomAccessibleInterval< U > images,
			final CrossCorrelationFactoryInterface<U, U, V > ccFactory,
			final int range,
			final long[] radius,
			final V dummy ) {
		final long dimX = images.dimension( 0 );
		final long dimY = images.dimension( 1 );
		final long dimZ = images.dimension( 2 );
		final ArrayImg< V, ? > store = new ArrayImgFactory< V >().create( new long[] { dimX,  dimY, dimZ, dimZ }, dummy );
		final V nanDummy = dummy.copy();
		final V oneDummy = dummy.copy();
		nanDummy.setReal( Double.NaN );
		oneDummy.setReal( 1.0 );
		final IntervalView<V> constantNaN = Views.interval( 
				Views.raster( new ConstantRealRandomAccesssible< V >( 2, nanDummy ) ), 
				Views.hyperSlice( Views.hyperSlice( store, 3, 0), 2, 0 ) );
		final IntervalView<V> constantOne = Views.interval( Views.raster( new ConstantRealRandomAccesssible< V >( 2, oneDummy ) ), constantNaN );
		for ( final V s : store )
			s.setReal( Double.NaN );
		for ( int z1 = 0; z1 < dimZ; ++z1 ) {
			final IntervalView<V> hs11 = Views.hyperSlice( store, 3, z1 );
			final IntervalView<V> hs12 = Views.hyperSlice( store, 2, z1 );
			final IntervalView<U> i1   = Views.hyperSlice( images, 2, z1 );
			for ( int z2 = 0; z2 < dimZ; ++z2 ) {
				final IntervalView<V> hs21 = Views.hyperSlice( hs11, 2, z2 );
				final IntervalView<U> i2   = Views.hyperSlice( images, 2, z2 );
				final RandomAccessibleInterval< V> rai;
				if ( Math.abs( z1 - z2 ) > range )
					rai = constantNaN;
				else if ( z1 == z2 )
					rai = constantOne;
				else if ( z1 > z2 )
					rai = Views.hyperSlice( hs12, 2, z2 );
				else 
					rai = ccFactory.create( i1, i2, radius);
				CopyFromIntervalToInterval.copyToRealType( rai, hs21 );
			}
		}
		return new DenseCorrelationStorage<V>( store );
	}
	
	@Override
	public RandomAccessibleInterval<T> toMatrix(final long x, final long y) {
		return Views.hyperSlice( Views.hyperSlice( store, 1, y), 0, x );
	}

	@Override
	public RandomAccess<T> randomAccess() {
		return store.randomAccess();
	}

	@Override
	public RandomAccess<T> randomAccess(final Interval interval) {
		return store.randomAccess(interval);
	}

	@Override
	public int numDimensions() {
		return store.numDimensions();
	}

	@Override
	public long min(final int d) {
		return store.min(d);
	}

	@Override
	public void min(final long[] min) {
		store.min( min );
	}

	@Override
	public void min(final Positionable min) {
		min( min );
	}

	@Override
	public long max(final int d) {
		return store.max( d );
	}

	@Override
	public void max(final long[] max) {
		store.max( max );
	}

	@Override
	public void max(final Positionable max) {
		store.max( max );
	}

	@Override
	public double realMin(final int d) {
		return store.realMin(d);
	}

	@Override
	public void realMin(final double[] min) {
		store.realMin(min);
	}

	@Override
	public void realMin(final RealPositionable min) {
		store.realMin(min);
	}

	@Override
	public double realMax(final int d) {
		return store.realMax(d);
	}

	@Override
	public void realMax(final double[] max) {
		store.realMax(max);
	}

	@Override
	public void realMax(final RealPositionable max) {
		store.realMax(max);
	}

	@Override
	public void dimensions(final long[] dimensions) {
		store.dimensions(dimensions);
	}

	@Override
	public long dimension(final int d) {
		return store.dimension(d);
	}
	
	public static void main(final String[] args) {
		final ImagePlus imp = new ImagePlus( "/data/hanslovskyp/jain-nobackup/234_data_downscaled/crop-100x100+100+200/data/data.tif" );
		final FloatImagePlus<FloatType> images = ImagePlusAdapter.wrapFloat( imp );
		
		for ( int i = 0; i < images.numDimensions(); ++i )
			System.out.println( images.dimension( i ) );	
		
		final FloatingPointIntegralCrossCorrelationFactory<FloatType, FloatType, FloatType> ccFactory = 
				new FloatingPointIntegralCrossCorrelationFactory< FloatType, FloatType, FloatType >( CrossCorrelationType.SIGNED_SQUARED, new FloatType() );
		final int range = 10;
		final long[] radius = new long[] { 10, 10 };
		final FloatType dummy = new FloatType();
		final DenseCorrelationStorage<FloatType> s = create(images, ccFactory, range, radius, dummy);
		final RandomAccessibleInterval<FloatType> m = s.toMatrix( 50, 50 );
		new ImageJ();
		ImageJFunctions.show( m );
		
	}

}
