/**
 * 
 */
package org.janelia.thickness.lut;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Abstract base class for LUT based transforms in 2D fields.
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
abstract public class AbstractLUTRealTransformField implements InvertibleRealTransform
{
	

	final protected int numSourceDimensions;
	final protected int numTargetDimensions;
	final protected int lutMaxIndex;
	final protected RandomAccessibleInterval< DoubleType > luts;
	final protected int transformDimension;
	final protected RandomAccess<DoubleType> access;
	
	
	public AbstractLUTRealTransformField( final double[] lut, final int numSourceDimensions, final int numTargetDimensions, final long ... dimensions )
	{
		assert lut.length == dimensions[ 2 ];
		
		this.transformDimension = 2;
		
		this.luts = ArrayImgs.doubles( dimensions );

		final ArrayCursor<DoubleType> cursor = ( (ArrayImg< DoubleType, DoubleArray > )this.luts ).cursor();
		
		while ( cursor.hasNext() ) {
			cursor.fwd();
			cursor.get().set( lut[ cursor.getIntPosition( transformDimension ) ] );
		}
		
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		
		this.access = this.luts.randomAccess();

		lutMaxIndex = lut.length - 1;
	}
	
	
	public AbstractLUTRealTransformField(final int numSourceDimensions,
			final int numTargetDimensions, final RandomAccessibleInterval< DoubleType > luts)
	{
		super();
		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.luts = luts;
		this.access = luts.randomAccess();
		this.transformDimension = 2;
		this.lutMaxIndex = (int) this.luts.dimension( this.transformDimension ) - 1;
	}
	
	
	protected double apply( final double z, final int x, final int y )
	{
		access.setPosition( x, 0 );
		access.setPosition( y, 1 );
		final int zFloor = ( int )z;
		access.setPosition( zFloor, 2 );
		final double floorVal = access.get().get();
		access.setPosition( zFloor + 1, 2);
		
		final double dz = z - zFloor;
		return ( access.get().get() - floorVal ) * dz + floorVal;
	}

	protected double applyChecked( final double z, final int x, final int y )
	{
		if ( z < 0 ) return -Double.MAX_VALUE;
		else if ( z >= lutMaxIndex ) return Double.MAX_VALUE;
		else return apply( z, x, y );
	}
	
	/**

	 * Implemented as bin-search.
	 * 
	 * @return
	 */
	protected int findFloorIndex( final double zPrime, final int x, final int y )
	{
		access.setPosition( x,  0 );
		access.setPosition( y, 1 );
		
		int min = 0;
		int max = lutMaxIndex;
		int i = max >> 1;
		do
		{
			access.setPosition( i, 2 );
			if ( access.get().get() > zPrime )
				max = i;
			else
				min = i;
			i = ( ( max - min ) >> 1 ) + min;
		}
		while ( i != min );
		return i;

	}
	
	protected double applyInverse( final double zPrime, final int x, final int y )
	{
		final int i = findFloorIndex( zPrime, x, y );
		
		access.setPosition( x, 0 );
		access.setPosition( y, 1 );
		access.setPosition( i, 2 );
		final double z1 = access.get().get();
		access.setPosition( i + 1, 2 );
		final double z2 = access.get().get();
		
		return ( zPrime - z1 )  / ( z2 - z1 ) + i;
	}
	
	protected double applyInverseChecked( final double zPrime, final int x, final int y )
	{
		access.setPosition( x, 0 );
		access.setPosition( y, 1 );
		access.setPosition( 0, 2 );
		final double lowVal = access.get().get();
		access.setPosition( this.lutMaxIndex, 2);
		final double highVal = access.get().get();
		if ( zPrime < lowVal )
			return -Double.MAX_VALUE;
		else if ( zPrime > highVal)
			return Double.MAX_VALUE;
		else
			return applyInverse( zPrime, x, y );
	}
	
	public double minTransformedCoordinate( final int x, final int y )
	{
		access.setPosition( x, 0 );
		access.setPosition( y, 1 );
		access.setPosition( 0, 2 );
		return access.get().get();
	}

	public double maxTransformedCoordinate( final int x, final int y )
	{
		access.setPosition( x, 0 );
		access.setPosition( y, 1 );
		access.setPosition( this.lutMaxIndex, 2 );
		return access.get().get();
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
}
