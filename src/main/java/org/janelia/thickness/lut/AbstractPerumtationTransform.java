/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.transform.InvertibleTransform;

/**
 * Bijective integer transform mapping between integer coordinates in [0,n-1].
 *
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public abstract class AbstractPerumtationTransform implements
		InvertibleTransform {
	
	final protected int[] lut;
	final protected int[] inverseLut;
	

	/**
	 * @param lut must be a bijective permutation over its index set, i.e. for a lut of legnth n,
	 * the sorted content the array must be [0,...,n-1] which is the index set of the lut.
	 * @param numSourceDimensions
	 * @param numTargetDimensions
	 */
	public AbstractPerumtationTransform( final int[] lut ) {
		super();
		this.lut = lut.clone();
		
		inverseLut = new int[ lut.length ];
		for ( int i = 0; i < lut.length; ++i )
		    inverseLut[ lut[ i ] ] = i;
	}
	
	
	public int apply( final int x )
	{
		return lut[ x ];
	}

	public long applyChecked( final int x )
	{
		if ( x < 0 ) return -Long.MAX_VALUE;
		else if ( x >= lut.length ) return Long.MAX_VALUE;
		else return apply( x );
	}

	public int applyInverse( final int y )
	{
		return inverseLut[ y ];
	}

	public long applyInverseChecked( final int y )
	{
	    if ( y < 0 ) return -Long.MAX_VALUE;
        else if ( y >= lut.length ) return Long.MAX_VALUE;
		else return applyInverse( y );
	}
	
	public int[] getLutCopy() {
		return lut.clone();
	}
	
	public int[] getInverseLutCopy() {
		return inverseLut.clone();
	}

	
}
