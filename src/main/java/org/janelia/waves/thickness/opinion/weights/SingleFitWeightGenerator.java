package org.janelia.waves.thickness.opinion.weights;

import java.util.Arrays;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

@SuppressWarnings("deprecation")
public class SingleFitWeightGenerator extends FitWeightGenerator {
	
	

	public SingleFitWeightGenerator(CurveFitter fitter,
			SymmetricDifferentiableParameterizedFunction func,
			double[] initialGuess) {
		super(fitter, func, initialGuess);
	}

	public void generate(double[] coordinates, double[] measurements, long x, long y,
			double zRef, long zMin, long zMax, long zPosition) {
		
		this.initialGuess[0] = zRef;
		
		for ( int examinationIndex = 0; examinationIndex < measurements.length; ++examinationIndex ) {
			
		
			this.fitter.clearObservations();
			
			for (int i = 0; i < measurements.length; ++i ) {
				double weight = this.getWeightFor( x, y, zMin + i);
				if ( i == examinationIndex ) {
					weight = 0.0;
				}
				this.fitter.addObservedPoint( weight, coordinates[i], measurements[i]);
			}
			
			try {
				this.initialGuess = this.fitter.fit( this.func, this.initialGuess );
			} catch (OptimizationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			
			long currPos = examinationIndex + zMin;
			
			TreeMap<Long, TreeMap<Long, Double> > opinionsAtXY = opinions.get( new ConstantPair<Long, Long>( x, y ) );
			
			if ( opinionsAtXY == null ) {
				opinionsAtXY = new TreeMap<Long, TreeMap<Long, Double> >();
				opinions.put( new ConstantPair<Long, Long>( x, y ), new TreeMap<Long, TreeMap<Long, Double> >() );
			}
			
			
			TreeMap<Long, Double> opinion = opinionsAtXY.get( currPos );
			
			if (opinion == null ) {
				opinion = new TreeMap<Long, Double>();
				opinionsAtXY.put( currPos, opinion );
			}
			
			
			try {
				
				double sum = 1.0;
				
				for ( int i = 0; i < measurements.length; ++i ) {
					if ( i == examinationIndex ) {
						continue;
					}
					sum += Math.abs( this.func.value( coordinates[i], this.initialGuess) - measurements[i] );
				}
				
				sum /= ( measurements.length - 1 );
				
				opinion.put( currPos, Math.abs( this.func.value( coordinates[examinationIndex], this.initialGuess ) - measurements[examinationIndex] ) / sum );
				
			} catch (FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException();
			}
			
		}

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
//			this.generate( zInterval, measurements, x, y, zInterval[ (int) (meta.zPosition - meta.zCoordinateMin) ], z - actualRangeLower,  z + actualRangeUpper, z );
		}
	}

}
