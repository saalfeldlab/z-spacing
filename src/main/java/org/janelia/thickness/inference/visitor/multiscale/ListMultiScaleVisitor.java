/**
 * 
 */
package org.janelia.thickness.inference.visitor.multiscale;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.inference.Options;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class ListMultiScaleVisitor extends AbstractListMultiScaleVisitor {

	public ListMultiScaleVisitor(final ArrayList<MultiScaleVisitor> visitors) {
		super(visitors);
	}

	@Override
	protected void actSelf(
			final int index,
			final RandomAccessibleInterval<DoubleType> lutField,
			final RandomAccessibleInterval<DoubleType> previousLutField,
			final long[] radii,
            final int[] steps,
			final Options options ) {
		// do not do anything, just act on all visitors
	}

}
