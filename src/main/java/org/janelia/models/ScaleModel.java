package org.janelia.models;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

public class ScaleModel extends AbstractModel<ScaleModel> {
	
	private static final long serialVersionUID = -1209207585882740130L;

	private float multiplier = 1.0f;

	public ScaleModel(final float multiplier) {
		super();
		this.multiplier = multiplier;
	}
	
	public ScaleModel() {
		this( 1.0f );
	}

	@Override
	public int getMinNumMatches() {
		return 1;
	}

	@Override
	public <P extends PointMatch> void fit(final Collection<P> matches)
			throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		// TODO Auto-generated method stub
		
		float XY = 0.0f;
		float XX = 0.0f;
		
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
	public float[] apply(final float[] location) {
		final float[] ret = new float[ location.length ];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = location[i] * this.multiplier;
		}
		return ret;
	}

	@Override
	public void applyInPlace(final float[] location) {
		for (int i = 0; i < location.length; i++) {
			location[i] *= this.multiplier;
		}
	}

}
