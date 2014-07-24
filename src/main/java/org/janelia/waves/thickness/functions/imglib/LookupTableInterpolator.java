package org.janelia.waves.thickness.functions.imglib;

import net.imglib2.EuclideanSpace;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * @author hanslovskyp
 *
 */
public class LookupTableInterpolator implements
RealRandomAccessible<DoubleType> {
	
	
	
	private final double[] values;
	private final double   shift;
	private final InterpolatorFactory< DoubleType, ArrayImg<DoubleType, DoubleArray> > interpolatorFactory;


	/**
	 * @param values
	 * @param shift
	 * @param interpolatorFactory
	 */
	public LookupTableInterpolator(
			double[] values,
			double shift,
			InterpolatorFactory<DoubleType, ArrayImg<DoubleType, DoubleArray>> interpolatorFactory) {
		super();
		this.values = values;
		this.shift = shift;
		this.interpolatorFactory = interpolatorFactory;
	}
	
	
	
	/**
	 * @param values  length must be odd number
	 * @param interpolatorFactory
	 */
	public LookupTableInterpolator(
			double[] values,
			InterpolatorFactory<DoubleType, ArrayImg<DoubleType, DoubleArray>> interpolatorFactory) {
		this( values, values.length % 2 == 0 ? values.length / 2.0 - 1.0 : values.length / 2.0, interpolatorFactory );
	}

	public int numDimensions() {
		return 1;
	}

	public RealRandomAccess<DoubleType> realRandomAccess() {
		return new LookupTableRandomAccess();
	}

	public RealRandomAccess<DoubleType> realRandomAccess(RealInterval interval) {
		return this.realRandomAccess();
	} 
	
	public class LookupTableRandomAccess extends RealPoint implements RealRandomAccess< DoubleType > {

		public DoubleType get() {
			ArrayImg<DoubleType, DoubleArray> arr         = ArrayImgs.doubles( values, values.length );
			RealRandomAccessible<DoubleType> interpolated = Views.interpolate( arr, interpolatorFactory );
			RealRandomAccess<DoubleType> randomAccess = interpolated.realRandomAccess();
			randomAccess.setPosition( new double[] { this.position[0] + shift } );
			return randomAccess.get();
		}

		public LookupTableRandomAccess copy() {
			LookupTableRandomAccess copy = new LookupTableRandomAccess();
			copy.setPosition( this );
			return copy;
		}

		public RealRandomAccess<DoubleType> copyRealRandomAccess() {
			return copy();
		}
		
	}

}
