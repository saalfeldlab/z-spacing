/**
 * 
 */
package org.janelia.thickness.inference.visitor.multiscale;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.thickness.inference.Options;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public abstract class AbstractListMultiScaleVisitor implements
		MultiScaleVisitor {
	
	protected final ArrayList< MultiScaleVisitor > visitors;
	
	/**
	 * @param visitors
	 */
	public AbstractListMultiScaleVisitor(final ArrayList<MultiScaleVisitor> visitors) {
		super();
		this.visitors = visitors;
	}
	
	public void addVisitor( final MultiScaleVisitor visitor ) {
		this.visitors.add( visitor );
	}


	@Override
	public void act(
			final int index,
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii,
            final int[] steps,
            final CorrelationsObjectInterface co,
			final Options options ) {
		for ( final MultiScaleVisitor v : visitors )
			v.act( index, lutField, previousLutField, radii, steps, co, options );
		actSelf( index, lutField, previousLutField, radii, steps, co, options );
	}
	
	
	protected abstract void actSelf(
			final int index,
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii,
            final int[] steps,
            final CorrelationsObjectInterface co,
			final Options options );
	
}
