package org.janelia.models;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;

@SuppressWarnings("deprecation")
public class FunctionFitModel extends AbstractModel<FunctionFitModel> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4212496677146397493L;
	private double[] parameters;
	private ParametricRealFunction func;
	private CurveFitter fitter;
	
	
	

	/**
	 * @return the parameters
	 */
	public double[] getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(final double[] parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the func
	 */
	public ParametricRealFunction getFunc() {
		return func;
	}

	/**
	 * @return the fitter
	 */
	public CurveFitter getFitter() {
		return fitter;
	}

	public FunctionFitModel(final double[] parameters, final ParametricRealFunction func,
			final CurveFitter fitter) {
		super();
		this.parameters = parameters;
		this.func = func;
		this.fitter = fitter;
	}

	@Override
	public int getMinNumMatches() {
		return this.parameters.length;
	}

	@Override
	public <P extends PointMatch> void fit(final Collection<P> matches)
			throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		if ( matches.size() < getMinNumMatches() ) {
			throw new NotEnoughDataPointsException( String.format( "Got %d, expected %d", matches.size(), getMinNumMatches() ) );
		}
		
		this.fitter.clearObservations();
		
		for ( final P m : matches ) {
			this.fitter.addObservedPoint( m.getWeight(), m.getP1().getW()[0], m.getP2().getW()[0] );
		}
		
		try {
			this.parameters = this.fitter.fit( this.func, this.parameters );
		} catch (final OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NotEnoughDataPointsException( e.getMessage() );
		} catch (final FunctionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllDefinedDataPointsException( e.getMessage() );
		} catch (final IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllDefinedDataPointsException( e.getMessage() );
		}
		
	}



	@Override
	public float[] apply(final float[] location) {
		final float[] result = new float[ location.length ];
		for (int i = 0; i < result.length; i++) {
			try {
				result[i] = (float) this.func.value( location[i], this.parameters );
			} catch (final FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	public void applyInPlace(final float[] location) {
		for (int i = 0; i < location.length; i++) {
			try {
				location[i] = (float) this.func.value( location[i], this.parameters );
			} catch (final FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}



	@Override
	public void set(final FunctionFitModel m) {
		this.parameters = m.parameters;
		this.func       = m.func;
		this.fitter     = m.fitter;
	}

	@Override
	public FunctionFitModel copy() {
		return new FunctionFitModel( parameters.clone(), func, fitter );
	}

}
