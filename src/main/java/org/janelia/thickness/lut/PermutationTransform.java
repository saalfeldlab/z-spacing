/**
 *
 */
package org.janelia.thickness.lut;

import java.util.Arrays;

import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.transform.InvertibleTransform;

/**
 * Bijective integer transform mapping between integer coordinates in [0,n-1].
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 */
public class PermutationTransform implements InvertibleTransform
{
	final protected int numSourceDimensions;
	final protected int numTargetDimensions;
	final protected int[] lut;
	final protected int[] inverseLut;

	/**
	 *
	 * @param lut must be bijective over its index.
	 * @param numSourceDimensions
	 * @param numTargetDimensions
	 */
	public PermutationTransform( final int[] lut, final int numSourceDimensions, final int numTargetDimensions )
	{
	    this.numSourceDimensions = numSourceDimensions;
        this.numTargetDimensions = numTargetDimensions;

        this.lut = lut.clone();

        System.out.println( Arrays.toString( lut ) );

		inverseLut = new int[ lut.length ];
		for ( int i = 0; i < lut.length; ++i )
		    inverseLut[ lut[ i ] ] = i;
	}

	protected int apply( final int x )
	{
		return lut[ x ];
	}

	protected long applyChecked( final int x )
	{
		if ( x < 0 ) return -Long.MAX_VALUE;
		else if ( x >= lut.length ) return Long.MAX_VALUE;
		else return apply( x );
	}

	protected int applyInverse( final int y )
	{
		return inverseLut[ y ];
	}

	protected long applyInverseChecked( final int y )
	{
	    if ( y < 0 ) return -Long.MAX_VALUE;
        else if ( y >= lut.length ) return Long.MAX_VALUE;
		else return applyInverse( y );
	}

	@Override
	public int numSourceDimensions()
	{
		return numSourceDimensions;
	}

	@Override
	public int numTargetDimensions()
	{
		return numTargetDimensions;
	}

	@Override
	public void apply( final long[] source, final long[] target )
	{
		assert source.length >= numTargetDimensions && target.length >= numTargetDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
		{
			System.out.println( source[ d ] + " > " + lut[ ( int )source[ d ] ] );
			target[ d ] = lut[ ( int )source[ d ] ];
		}
	}

	@Override
	public void apply( final int[] source, final int[] target )
	{
		assert source.length >= numTargetDimensions && target.length >= numTargetDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
			target[ d ] = lut[ source[ d ] ];
	}

	@Override
	public void apply( final Localizable source, final Positionable target )
	{
		assert source.numDimensions() >= numTargetDimensions && target.numDimensions() >= numTargetDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numTargetDimensions; ++d )
			target.setPosition( lut[ source.getIntPosition( d ) ], d );
	}

	@Override
	public void applyInverse( final long[] source, final long[] target )
	{
		assert source.length >= numSourceDimensions && target.length >= numSourceDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source[ d ] = inverseLut[ ( int )target[ d ] ];
	}

	@Override
	public void applyInverse( final int[] source, final int[] target )
	{
		assert source.length >= numSourceDimensions && target.length >= numSourceDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source[ d ] = inverseLut[ target[ d ] ];
	}

	@Override
	public void applyInverse( final Positionable source, final Localizable target )
	{
		assert source.numDimensions() >= numSourceDimensions && target.numDimensions() >= numSourceDimensions : "Dimensions do not match.";

		for ( int d = 0; d < numSourceDimensions; ++d )
			source.setPosition( inverseLut[ target.getIntPosition( d ) ], d );
	}

	@Override
	public InvertibleTransform inverse()
	{
		return new PermutationTransform( inverseLut, numTargetDimensions, numSourceDimensions );
	}
}
