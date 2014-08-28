package org.janelia.utility.benchmark;

import net.imglib2.realtransform.InvertibleRealTransform;

/**
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */

public class InverseTransformExecutor implements ExecutionFunctor {

	/**
	 * @param source
	 * @param target
	 * @param tf
	 */
	public InverseTransformExecutor(final double[] source, 
			final double[] target,
			final InvertibleRealTransform tf) {
		super();
		this.source = source;
		this.target = target;
		this.tf = tf;
	}

	private final double[] source;
	private final double[] target;
	private final InvertibleRealTransform tf;

	@Override
	public void run() {
		tf.applyInverse(source, target);
	}

}
