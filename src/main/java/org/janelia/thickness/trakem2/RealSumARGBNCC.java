package org.janelia.thickness.trakem2;

import java.util.concurrent.Callable;

import net.imglib2.util.RealSum;

/**
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 *
 */
public class RealSumARGBNCC implements Callable< Double >
{
	protected int[] ap;
	protected int[] bp;

	public RealSumARGBNCC( final int[] ap, final int[] bp )
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
			final int ra = ( ap[ i ] >> 16 ) & 0xff;
			final int ga = ( ap[ i ] >> 8 ) & 0xff;
			final int ba = ap[ i ] & 0xff;
			final int rb = ( bp[ i ] >> 16 ) & 0xff;
			final int gb = ( bp[ i ] >> 8 ) & 0xff;
			final int bb = bp[ i ] & 0xff;

			if (
					ra == 0 || ga == 0 || ba == 0 ||
					rb == 0 || gb == 0 || bb == 0 ||
					ra == 255 || ga == 255 || ba == 255 ||
					rb == 255 || gb == 255 || bb == 255 )
				continue;

			++n;
			final double va = 0.3 * ra + 0.6 * ga + 0.1 * ba;
			final double vb = 0.3 * rb + 0.6 * gb + 0.1 * bb;
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
