package org.janelia.models;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

public class ScaleModel extends AbstractModel<ScaleModel> {

	private static final long serialVersionUID = -1209207585882740130L;

	private double multiplier = 1.0;

	public ScaleModel(final double multiplier) {
		super();
		this.multiplier = multiplier;
	}

	public ScaleModel() {
		this( 1.0 );
	}

	@Override
	public int getMinNumMatches() {
		return 1;
	}

	@Override
	public <P extends PointMatch> void fit(final Collection<P> matches)
			throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		// TODO Auto-generated method stub

		double XY = 0.0;
		double XX = 0.0;

		for ( final P m : matches ) {

			XY += m.getP1().getL()[0] * m.getP2().getL()[0] * m.getWeight();
			XX += m.getP1().getL()[0] * m.getP1().getL()[0] * m.getWeight();

		}

		this.multiplier = XY / XX;

	}

	@Override
	public void set(final ScaleModel m) {
		this.multiplier = m.multiplier;
	}

	@Override
	public ScaleModel copy() {
		return new ScaleModel( this.multiplier );
	}

	@Override
	public double[] apply(final double[] location) {
		final double[] ret = new double[ location.length ];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = location[i] * this.multiplier;
		}
		return ret;
	}

	@Override
	public void applyInPlace(final double[] location) {
		for (int i = 0; i < location.length; i++) {
			location[i] *= this.multiplier;
		}
	}

}
