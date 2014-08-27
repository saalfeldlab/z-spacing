/**
 * 
 */
package org.janelia.utility.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InvertibleRealTransform;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ScaleAndShift implements InvertibleRealTransform {
	
	private final ScaleAndShift inverse;
	private final double[] scales;
	private final double[] shifts;
	private final int nDim;
	
	

	/**
	 * @param inverse
	 * @param scales
	 * @param shifts
	 * @param nDim
	 */
	public ScaleAndShift(final ScaleAndShift inverse, final double[] scales,
			final double[] shifts, final int nDim) {
		super();
		this.inverse = inverse;
		this.scales = scales;
		this.shifts = shifts;
		this.nDim = nDim;
	}

	/**
	 * @param shifts
	 * @param scales
	 */
	public ScaleAndShift( final double[] scales, final double[] shifts ) {
		super();
		assert shifts.length == scales.length;
		this.scales = scales.clone(); // clone?
		this.shifts = shifts.clone(); // clone?
		this.nDim   = shifts.length;
		this.inverse = this.inverse();
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.RealTransform#numSourceDimensions()
	 */
	@Override
	public int numSourceDimensions() {
		return nDim;
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.RealTransform#numTargetDimensions()
	 */
	@Override
	public int numTargetDimensions() {
		return nDim;
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.RealTransform#apply(double[], double[])
	 */
	@Override
	public void apply(final double[] source, final double[] target) {
		assert source.length == nDim && target.length == nDim;
		for (int i = 0; i < nDim; i++) {
			target[i] = scales[i]*source[i] + shifts[i];
		}
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.RealTransform#apply(float[], float[])
	 */
	@Override
	public void apply(final float[] source, final float[] target) {
		assert source.length == nDim && target.length == nDim;
		for (int i = 0; i < nDim; i++) {
			target[i] = (float) (scales[i]*source[i] + shifts[i]);
		}
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.RealTransform#apply(net.imglib2.RealLocalizable, net.imglib2.RealPositionable)
	 */
	@Override
	public void apply(final RealLocalizable source, final RealPositionable target) {
		assert source.numDimensions() == nDim && target.numDimensions() == nDim;
		for ( int d = 0; d < nDim; ++d ) {
			target.setPosition( scales[d]*source.getDoublePosition( d ) + shifts[d], d);
		}
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.InvertibleRealTransform#applyInverse(double[], double[])
	 */
	@Override
	public void applyInverse(final double[] source, final double[] target) {
		// target is the source for the inverse transform, thus switch order in call of this.inverse.apply
		this.inverse.apply( target, source );
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.InvertibleRealTransform#applyInverse(float[], float[])
	 */
	@Override
	public void applyInverse(final float[] source, final float[] target) {
		// target is the source for the inverse transform, thus switch order in call of this.inverse.apply
		this.inverse.apply( target, source );
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.InvertibleRealTransform#applyInverse(net.imglib2.RealPositionable, net.imglib2.RealLocalizable)
	 */
	@Override
	public void applyInverse(final RealPositionable source, final RealLocalizable target) {
		// target is the source for the inverse transform, thus switch order in call of this.inverse.apply
		this.inverse.apply( target, source );
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.InvertibleRealTransform#inverse()
	 */
	@Override
	public ScaleAndShift inverse() {
		final double[] invertedShifts = new double[ nDim ];
		final double[] invertedScales = new double[ nDim ];
		for (int i = 0; i < nDim; i++) {
			invertedScales[i] = 1.0 /scales[i];
			invertedShifts[i] = -shifts[i] * invertedScales[i];
		}
		return new ScaleAndShift( this, invertedScales, invertedShifts, nDim );
	}

	/* (non-Javadoc)
	 * @see net.imglib2.realtransform.InvertibleRealTransform#copy()
	 */
	@Override
	public ScaleAndShift copy() {
		return new ScaleAndShift(inverse, scales, shifts, nDim );
	}

}
