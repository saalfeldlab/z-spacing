package org.janelia.waves.thickness.v2;

import net.imglib2.RandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class FitWithGradient {
	
	private final ArrayImg< DoubleType, DoubleArray > gradientValues;
	private final ArrayImg< DoubleType, DoubleArray > fitValues;
	private final InterpolatorFactory<DoubleType, RandomAccessible<DoubleType> > interpolatorFactory;
	
	public static interface GradientCalculator {
		public void calculate( ArrayImg<DoubleType, DoubleArray> fitValues, ArrayImg<DoubleType, DoubleArray> gradient );
	}
	
	public static class SymmetricGradient implements GradientCalculator {

		@Override
		public void calculate(final ArrayImg<DoubleType, DoubleArray> fitValues,
				final ArrayImg<DoubleType, DoubleArray> gradient) {
			
			final ArrayRandomAccess<DoubleType> fitAccess      = fitValues.randomAccess();
			final ArrayRandomAccess<DoubleType> gradientAccess = gradient.randomAccess();
			
			gradientAccess.setPosition( 0, 0 );
			gradientAccess.get().set( 0.0 );
			
			
			for ( int i = 1; i < fitValues.dimension( 0 ) - 1; ++i ) {
				
				gradientAccess.setPosition( i, 0 );
				
				fitAccess.setPosition( i + 1, 0 );
				gradientAccess.get().add( fitAccess.get() );
				
				fitAccess.setPosition( i - 1, 0 );
				gradientAccess.get().sub( fitAccess.get() );
				
				gradientAccess.get().mul( 1.0 / 2.0 );
				
			}
			
			final ArrayRandomAccess<DoubleType> gradientAccess2 = gradient.randomAccess();
			gradientAccess2.setPosition( gradient.dimension( 0 ) -1, 0 );
			gradientAccess2.get().set( gradientAccess.get() );
			
		}
		
	}
	
	
	public static class SymmetricRealRandomAccessible implements RealRandomAccessible< DoubleType > {
		
		private final boolean invert;
		private final ArrayImg< DoubleType, DoubleArray > baseData;
		private final RealRandomAccessible< DoubleType > viewData;

		public SymmetricRealRandomAccessible( final boolean invert, final ArrayImg< DoubleType, DoubleArray > baseData, final InterpolatorFactory< DoubleType, RandomAccessible<DoubleType> > factory ) {
			super();
			this.invert = invert;
			
			this.baseData = baseData;
			this.viewData = Views.interpolate( Views.extendValue( baseData, new DoubleType( Double.NaN ) ), factory);
		}

		@Override
		public int numDimensions() {
			return baseData.numDimensions();
		}

		public  class SymmetricRealRandomAccess extends RealPoint implements RealRandomAccess< DoubleType > {
			
			private final RealRandomAccess< DoubleType > access;
			

			private SymmetricRealRandomAccess() {
				super( baseData.numDimensions() );
				this.access = viewData.realRandomAccess();
			}

			@Override
			public DoubleType get() {
				
				this.access.setPosition( Math.abs( this.position[0] ), 0 );
				final double value = this.access.get().get();
				
				if ( this.position[0] < 0.0 && invert ) {
					return new DoubleType( - value );
				} else {
					return new DoubleType( value );
				}
			}

			// need to clarify use of copy() and copyRealRandomAccess()
			@Override
			public Sampler<DoubleType> copy() {
				return new SymmetricRealRandomAccess();
			}

			// is baseData known here?
			@Override
			public RealRandomAccess<DoubleType> copyRealRandomAccess() {
				return new SymmetricRealRandomAccess();
			}
			
			
			
		}

		@Override
		public RealRandomAccess<DoubleType> realRandomAccess(final RealInterval interval) {
			return this.realRandomAccess();
		}

		@Override
		public RealRandomAccess<DoubleType> realRandomAccess() {
			return new SymmetricRealRandomAccess();
		}
		
	}
	
	public FitWithGradient( final ArrayImg<DoubleType, DoubleArray> fitValues, final GradientCalculator calc, final InterpolatorFactory<DoubleType, RandomAccessible<DoubleType> > interpolatorFactory ) {
		super();
		
		this.fitValues           = fitValues;
		this.gradientValues      = ArrayImgs.doubles( fitValues.dimension( 0 ) );
		this.interpolatorFactory = interpolatorFactory;
		
		calc.calculate( this.fitValues, this.gradientValues );
	}
	
	
	public RealRandomAccessible< DoubleType > getFit() {
		return new SymmetricRealRandomAccessible( false, this.fitValues, this.interpolatorFactory );
	}
	
	
	public RealRandomAccessible< DoubleType > getGradient() {
		return new SymmetricRealRandomAccessible( true, this.gradientValues, this.interpolatorFactory );
	}
	
	
	
	
	
	public static void main(final String[] args) {
		
		final double[] values = new double[] { 5, 4, 2, 1.5, 0 };
		
		final FitWithGradient fwg = new FitWithGradient( ArrayImgs.doubles( values, values.length ), new FitWithGradient.SymmetricGradient(), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealRandomAccess<DoubleType> fitAccess      = fwg.getFit().realRandomAccess();
		final RealRandomAccess<DoubleType> gradientAccess = fwg.getGradient().realRandomAccess();
		
		for ( int i = -values.length + 1; i < values.length; ++ i ) {
			final double j = i + 0.5;
			fitAccess.setPosition( j, 0 );
			gradientAccess.setPosition( j, 0);
			System.out.println( j + ": f=" + fitAccess.get().get() + ", df/dz=" + gradientAccess.get().get() );
		}
		
	}
	
	

}
