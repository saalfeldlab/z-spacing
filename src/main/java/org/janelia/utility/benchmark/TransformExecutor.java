package org.janelia.utility.benchmark;

import net.imglib2.realtransform.RealTransform;

/**
 * 
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */

public class TransformExecutor implements ExecutionFunctor {

	/**
	 * @param source
	 * @param target
	 * @param tf
	 */
	public TransformExecutor(final double[] source, 
			final double[] target,
			final RealTransform tf) {
		super();
		this.source = source;
		this.target = target;
		this.tf = tf;
	}

	private final double[] source;
	private final double[] target;
	private final RealTransform tf;

	@Override
	public void run() {
		tf.apply(source, target);
	}

}
