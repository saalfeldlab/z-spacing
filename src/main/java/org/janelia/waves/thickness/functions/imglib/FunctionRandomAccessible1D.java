package org.janelia.waves.thickness.functions.imglib;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;

import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;

public class FunctionRandomAccessible1D implements
		RealRandomAccessible<DoubleType> {
	
	private final double[] parameters;
	
	private final ParametricRealFunction func;

	public FunctionRandomAccessible1D(double[] parameters,
			ParametricRealFunction func) {
		super();
		this.parameters = parameters;
		this.func = func;
	}

	public int numDimensions() {
		// TODO Auto-generated method stub
		return 1;
	}

	public RealRandomAccess<DoubleType> realRandomAccess() {
		return new FunctionRandomAccess();
	}

	public RealRandomAccess<DoubleType> realRandomAccess(RealInterval interval) {
		return this.realRandomAccess();
	}
	
	public class FunctionRandomAccess extends RealPoint implements RealRandomAccess< DoubleType > {
		
		
		
		public FunctionRandomAccess() {
			super( 1 );
			this.t = new DoubleType();
		}

		protected final DoubleType t;

		public DoubleType get() {
			try {
				t.set( func.value( this.position[0], parameters ) );
			} catch (FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return t;
		}

		public FunctionRandomAccess copy() {
			FunctionRandomAccess copy = new FunctionRandomAccess();
			copy.setPosition( this );
			return copy;
		}

		public RealRandomAccess<DoubleType> copyRealRandomAccess() {
			return copy();
		}
		
	}

}
