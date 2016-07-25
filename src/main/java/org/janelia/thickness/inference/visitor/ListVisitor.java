/**
 *
 */
package org.janelia.thickness.inference.visitor;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ListVisitor implements Visitor
{
	private final ArrayList< Visitor > visitors;

	public ListVisitor()
	{
		super();
		this.visitors = new ArrayList< Visitor >();
	}

	public ListVisitor( final ArrayList< Visitor > visitors )
	{
		super();
		this.visitors = visitors;
	}

	public void addVisitor( final Visitor visitor )
	{
		visitors.add( visitor );
	}

	public ArrayList< Visitor > getVisitors()
	{
		return visitors;
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{
		for ( final Visitor v : visitors )
			v.act( iteration, matrix, scaledMatrix, lut, permutation, inversePermutation, multipliers, estimatedFit );
	}

}
