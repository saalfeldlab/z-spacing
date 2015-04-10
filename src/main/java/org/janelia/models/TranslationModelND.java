/**
 *
 */
package org.janelia.models;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;


/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class TranslationModelND extends AbstractModel< TranslationModelND > {

	final protected int MIN_NUM_MATCHES = 1;

	protected double[] t;

	/**
	 * @param t
	 */
	public TranslationModelND(final double[] t) {
		super();
		this.t = t;
	}


	/**
	 * @param numDimensions
	 */
	public TranslationModelND( final int numDimensions ) {
		this( new double[ numDimensions ] );
	}

	@Override
	public double[] apply(final double[] location) {

		final double[] result = location.clone();
		this.applyInPlace( result );
		return result;
	}

	@Override
	public void applyInPlace(final double[] location) {
		assert location.length >= this.t.length: "Dimension mismatch!";
		for (int i = 0; i < location.length; i++) {
			location[i] += this.t[i];
		}
	}


	@Override
	public int getMinNumMatches() {
		return this.MIN_NUM_MATCHES;
	}


	@Override
	public void set(final TranslationModelND m) {
		this.t = m.t.clone();
	}


	@Override
	public <P extends PointMatch> void fit(final Collection<P> matches)
			throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final int nMatches = matches.size();
		if ( nMatches < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " not enough data points to "
					+ String.format( "estimate a %dd translation model, at least ", this.t.length )
					+ MIN_NUM_MATCHES + " data points required." );
		// calculate center of mass
		final double[] sourceCOM = new double[ this.t.length ];
		final double[] targetCOM = new double[ this.t.length ];

		double weightSum = 0.0;

		for ( final P match : matches) {
			final double[] s = match.getP1().getL();
			final double[] t = match.getP2().getW();
			final double w = match.getWeight();
			weightSum += w;

			for (int i = 0; i < t.length; i++) {
				sourceCOM[i] += w*s[i];
				targetCOM[i] += w*t[i];
			}
		}

		final double inverseWeightSum = 1.0/weightSum;

		for (int i = 0; i < this.t.length; i++) {
			this.t[i] = inverseWeightSum * ( targetCOM[i] - sourceCOM[i] );
		}

	}


	@Override
	public TranslationModelND copy() {
		return new TranslationModelND( this.t.clone() );
	}



}
