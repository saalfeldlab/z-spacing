/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.Localizable;
import net.imglib2.Positionable;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 *         Apply bijective permutation to one coordinate axis only.
 *
 */
public class SingleDimensionPermutationTransform extends
		AbstractPerumtationTransform
{

	final protected int numSourceDimensions;

	final protected int numTargetDimensions;

	final protected int d;

	/**
	 * @param lut
	 * @param numSourceDimensions
	 *            dimensionality of source
	 * @param numTargetDimensions
	 *            dimensionality of target
	 * @param d
	 *            dimension which shall be transformed. Must be smaller than
	 *            {@link #numSourceDimensions} and {@link #numTargetDimensions}
	 */
	public SingleDimensionPermutationTransform(
			final int[] lut,
			final int numSourceDimensions,
			final int numTargetDimensions,
			final int d )
	{
		super( lut );
		assert d > 0 && d < numTargetDimensions && d < numSourceDimensions;
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.d = d;
	}

	@Override
	public void applyInverse( final long[] source, final long[] target )
	{
		System.arraycopy( target, 0, source, 0, this.numSourceDimensions );
		source[ d ] = applyInverse( ( int ) target[ d ] );
	}

	@Override
	public void applyInverse( final int[] source, final int[] target )
	{
		System.arraycopy( target, 0, source, 0, this.numSourceDimensions );
		source[ d ] = applyInverse( target[ d ] );
	}

	@Override
	public void applyInverse( final Positionable source, final Localizable target )
	{
		source.setPosition( target );
		source.setPosition( applyInverse( target.getIntPosition( d ) ), d );
	}

	@Override
	public SingleDimensionPermutationTransform inverse()
	{
		return new SingleDimensionPermutationTransform( inverseLut, numSourceDimensions, numTargetDimensions, d );
	}

	@Override
	public int numSourceDimensions()
	{
		return this.numSourceDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return this.numTargetDimensions;
	}

	@Override
	public void apply( final long[] source, final long[] target )
	{
		System.arraycopy( source, 0, target, 0, this.numTargetDimensions );
		target[ d ] = apply( ( int ) source[ d ] );
	}

	@Override
	public void apply( final int[] source, final int[] target )
	{
		System.arraycopy( source, 0, target, 0, this.numTargetDimensions );
		target[ d ] = apply( source[ d ] );
	}

	@Override
	public void apply( final Localizable source, final Positionable target )
	{
		target.setPosition( source );
		target.setPosition( apply( source.getIntPosition( d ) ), d );
	}

}
