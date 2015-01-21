/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.AbstractLUTRealTransform;

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

	abstract < T extends RealType< T > > void actSelf( final int iteration, 
			final RandomAccessibleInterval< T > matrix, final double[] lut,
			final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions );
	
	@Override
	public < T extends RealType< T > > void act( final int iteration, final RandomAccessibleInterval< T > matrix, final double[] lut,
			final AbstractLUTRealTransform transform,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit,
			final int[] positions ) {
		for ( final Visitor v : visitors ) {
			v.act(iteration, matrix, lut, transform, multipliers, weights, estimatedFit, positions );
		}
		actSelf(iteration, matrix, lut, transform, multipliers, weights, estimatedFit, positions );
	}
	
}
