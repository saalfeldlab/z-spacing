package org.janelia.thickness.lut;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.numeric.real.DoubleType;

public class LUTGrid extends AbstractLUTGrid {

	public LUTGrid(
			final int numSourceDimensions, 
			final int numTargetDimensions,
			final ArrayImg<DoubleType, DoubleArray> lutArray) {
		super(numSourceDimensions, 
				numTargetDimensions, 
				lutArray);
	}

	@Override
	public void apply(final double[] source, final double[] target) {
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			target[d] = source[d];
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.length; ++d) {
			target[d] = this.apply( source[d] );
		}
	}

	@Override
	public void apply(final float[] source, final float[] target) {
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			target[d] = source[d];
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.length; ++d) {
			target[d] = (float) this.apply( source[d] );
		}
	}
	
	@Override
	public void apply(final RealLocalizable source, final RealPositionable target) {
		this.updateCoordinates( source );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			target.setPosition( source.getDoublePosition( d ), d);
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.numDimensions(); ++d ) {
			target.setPosition( this.apply( source.getDoublePosition( d ) ), d);
		}
	}

	@Override
	public void applyInverse(final double[] source, final double[] target) {
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			source[d] = target[d];
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.length; ++d ) {
			source[d] = this.applyInverse( target[d] );
		}
	}

	@Override
	public void applyInverse(final float[] source, final float[] target) {
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			source[d] = target[d];
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.length; ++d ) {
			source[d] = (float) this.applyInverse( target[d] );
		}
	}

	@Override
	public void applyInverse(final RealPositionable source, final RealLocalizable target) {
		this.updateCoordinates( target );
		for ( int d = 0; d < this.nNonTransformedCoordinates; ++d ) {
			source.setPosition( target.getDoublePosition( d ), d );
		}
		for ( int d = this.nNonTransformedCoordinates; d < target.numDimensions(); ++d ) {
			source.setPosition( this.applyInverse( target.getDoublePosition( d ) ), d );
		}
	}

	@Override
	public InvertibleRealTransform inverse() {
		return new InverseRealTransform( this );
	}

	@Override
	public InvertibleRealTransform copy() {
		return new LUTGrid(numSourceDimensions, numTargetDimensions, lutArray);
	}


	

}
