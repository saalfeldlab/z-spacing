/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.util.ArrayList;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.FitWithGradient;
import org.janelia.thickness.lut.LUTRealTransform;

/**
 * @author hanslovskyp
 *
 */
public abstract class AbstractMultiVisitor implements Visitor {
	private final ArrayList< Visitor > visitors;
	
	
	
	public AbstractMultiVisitor() {
		super();
		this.visitors = new ArrayList<Visitor>();
	}

	public AbstractMultiVisitor(final ArrayList<Visitor> visitors) {
		super();
		this.visitors = visitors;
	}
	
	public void addVisitor( final Visitor visitor ) {
		visitors.add( visitor );
	}

	abstract void actSelf( final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
	final LUTRealTransform transform,
	final ArrayImg<DoubleType, DoubleArray> multipliers,
	final ArrayImg<DoubleType, DoubleArray> weights,
	final FitWithGradient fitWithGradient );
	
	public void act( final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
			final LUTRealTransform transform,
			final ArrayImg<DoubleType, DoubleArray> multipliers,
			final ArrayImg<DoubleType, DoubleArray> weights,
			final FitWithGradient fitWithGradient) {
		for ( final Visitor v : visitors ) {
			v.act(iteration, matrix, lut, transform, multipliers, weights, fitWithGradient);
		}
		actSelf(iteration, matrix, lut, transform, multipliers, weights, fitWithGradient);
	}
	
}
