package org.janelia.waves.thickness.v2.inference.visitor;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.waves.thickness.v2.FitWithGradient;
import org.janelia.waves.thickness.v2.LUTRealTransform;

public interface Visitor {

	public void act(int iteration, ArrayImg<DoubleType, DoubleArray> matrix, double[] lut,
			LUTRealTransform transform,
			ArrayImg<DoubleType, DoubleArray> multipliers,
			ArrayImg<DoubleType, DoubleArray> weights,
			FitWithGradient fitWithGradient);
}
