package org.janelia.thickness.inference.visitor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

public interface Visitor {

	public < T extends RealType< T > > void act(int iteration, RandomAccessibleInterval< T > matrix, double[] lut,
			AbstractLUTRealTransform transform,
			double[] multipliers,
			double[] weights,
			double[] estimatedFit,
			int[] positions );
}
