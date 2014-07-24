package org.janelia.waves.thickness.opinion.weights;

import java.util.Arrays;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

public abstract class FitWeightGenerator implements DataBasedWeightGenerator {
	
	private final double strength;
	
	protected final CurveFitter fitter;
	protected final SymmetricDifferentiableParameterizedFunction func;
	protected final TreeMap< ConstantPair<Long,Long>, TreeMap<Long, TreeMap<Long, Double> > > opinions;
	protected double[] initialGuess;
	
	
	

	public FitWeightGenerator(CurveFitter fitter,
			SymmetricDifferentiableParameterizedFunction func,
			double[] initialGuess) {
		super();
		this.fitter = fitter;
		this.func = func;
		this.initialGuess = initialGuess;
		this.opinions = new TreeMap< ConstantPair<Long,Long>, TreeMap<Long, TreeMap<Long, Double> > >();
		this.strength = 1.0;
	}



	public double getWeightFor(long x, long y, long zBin) {
		TreeMap<Long, TreeMap<Long, Double> > opinionsAtXY = opinions.get( new ConstantPair<Long, Long>( x, y ) );
		
		if ( opinionsAtXY == null ) {
			opinionsAtXY = new TreeMap<Long, TreeMap<Long, Double> >();
			opinions.put( new ConstantPair<Long, Long>( x, y ), new TreeMap<Long, TreeMap<Long, Double> >() );
		}
		
		TreeMap<Long, Double> opinion = opinionsAtXY.get( zBin );
		
		double result = 0.0;
		if ( opinion == null ) {
			result = 0.0;
		} else {
			for ( Double o : opinion.values() ) {
				result += o;
			}
			result /= opinion.size();
		}
		
		return Math.pow( 1.0 - result, this.strength );
//		return Math.pow( 1.0 / ( 1.0 + 1.0*result ), this.strength );
//		return Math.exp( this.strength * ( - result ) );
	}
	
	public void generateAtXY(double[] zAxis,
			CorrelationsObjectInterface correlations, long zBinMin, long zBinMax, long zStart, long zEnd,
			long x, long y) {
		generateAtXY(zAxis, correlations, zBinMin, zBinMax, zStart, zEnd, -1, x, y);
	}

	public void generateAtXY(double[] zAxis,
			CorrelationsObjectInterface correlations, long zBinMin, long zBinMax, long zStart, long zEnd, int range,
			long x, long y) {
		int N         = (int) (zEnd - zStart);
		int offset    = (int) (zStart - zBinMin);
		
		
		for ( int i = 0; i < N; ++i ) {
			
			int z = offset + i;
			
			Meta meta = correlations.getMetaMap().get( zStart + i );
			
			int actualRangeUpper = range;
			int actualRangeLower = range;
			
			if ( range < 0 ) {
				actualRangeUpper = (int) (meta.zCoordinateMax - meta.zPosition);
				actualRangeLower = (int) (meta.zPosition - meta.zCoordinateMin);
			}
			
			
			int nElements = actualRangeUpper + actualRangeLower;
			
			double[] zInterval  = new double[ nElements ];
			double[] measurements = new double[ nElements ];
			
			Cursor<DoubleType> corr = Views.iterable( correlations.extractDoubleCorrelationsAt( x, y, zStart + i ).getA() ).cursor();
			
			corr.jumpFwd( meta.zPosition - actualRangeLower - meta.zCoordinateMin );
			
			
			for ( int k = 0; k < nElements; ++k ) {
				zInterval[ k ] = zAxis[ z + k - actualRangeLower ];
				measurements[ k ] = corr.next().get();
			}
			
			this.generate( zInterval, measurements, x, y, zInterval[ (int) (meta.zPosition - meta.zCoordinateMin) ], meta.zCoordinateMin,  meta.zCoordinateMax, meta.zPosition );
		}
	}

}
