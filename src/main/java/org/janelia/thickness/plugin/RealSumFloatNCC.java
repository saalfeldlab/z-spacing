package org.janelia.thickness.plugin;

import java.util.concurrent.Callable;

import net.imglib2.util.RealSum;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public class RealSumFloatNCC implements Callable< Double >
{
	protected float[] ap;

	protected float[] bp;

	public RealSumFloatNCC( final float[] ap, final float[] bp )
	{
		this.ap = ap;
		this.bp = bp;
	}

	@Override
	public Double call()
	{
		final RealSum sumA = new RealSum();
		final RealSum sumAA = new RealSum();
		final RealSum sumB = new RealSum();
		final RealSum sumBB = new RealSum();
		final RealSum sumAB = new RealSum();
		int n = 0;
		for ( int i = 0; i < ap.length; ++i )
		{

			final double va = ap[ i ];
			final double vb = bp[ i ];

			if ( Double.isNaN( va ) || Double.isNaN( vb ) )
				continue;

			++n;
			sumA.add( va );
			sumAA.add( va * va );
			sumB.add( vb );
			sumBB.add( vb * vb );
			sumAB.add( va * vb );
		}
		final double suma = sumA.getSum();
		final double sumaa = sumAA.getSum();
		final double sumb = sumB.getSum();
		final double sumbb = sumBB.getSum();
		final double sumab = sumAB.getSum();

		return ( n * sumab - suma * sumb ) / Math.sqrt( n * sumaa - suma * suma ) / Math.sqrt( n * sumbb - sumb * sumb );
	}
}
