package org.janelia.models;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

public class ScaleModel extends AbstractModel<ScaleModel> {
	
	private float multiplier = 1.0f;

	public ScaleModel(float multiplier) {
		super();
		this.multiplier = multiplier;
	}
	
	public ScaleModel() {
		this( 0.0f );
	}

	public int getMinNumMatches() {
		return 1;
	}

	public <P extends PointMatch> void fit(Collection<P> matches)
			throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		// TODO Auto-generated method stub
		
		float XY = 0.0f;
		float XX = 0.0f;
		
		for ( P m : matches ) {
			
			XY += m.getP1().getL()[0] * m.getP2().getL()[0] * m.getWeight();
			XX += m.getP1().getL()[0] * m.getP1().getL()[0] * m.getWeight();
			
		}
		
		this.multiplier = XY / XX;
		
	}

	public void set(ScaleModel m) {
		this.multiplier = m.multiplier;
	}

	public ScaleModel copy() {
		return new ScaleModel( this.multiplier );
	}

	public float[] apply(float[] location) {
		float[] ret = new float[ location.length ];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = location[i] * this.multiplier;
		}
		return ret;
	}

	public void applyInPlace(float[] location) {
		for (int i = 0; i < location.length; i++) {
			location[i] *= this.multiplier;
		}
	}

}
