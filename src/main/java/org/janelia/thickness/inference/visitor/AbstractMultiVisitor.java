/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

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

	abstract < T extends RealType< T > > void actSelf(
			int iteration, 
			RandomAccessibleInterval< T > matrix, 
			double[] lut,
			int[] permutation,
			int[] inversePermutation,
			double[] multipliers,
			double[] weights,
			double[] estimatedFit
			);
	
	@Override
	public < T extends RealType< T > > void act( 
			final int iteration, 
			final RandomAccessibleInterval< T > matrix, 
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final double[] weights,
			final double[] estimatedFit
			) {
		for ( final Visitor v : visitors ) {
			v.act(iteration, matrix, lut, permutation, inversePermutation, multipliers, weights, estimatedFit );
		}
		actSelf(iteration, matrix, lut, permutation, inversePermutation, multipliers, weights, estimatedFit );
	}
	
}
