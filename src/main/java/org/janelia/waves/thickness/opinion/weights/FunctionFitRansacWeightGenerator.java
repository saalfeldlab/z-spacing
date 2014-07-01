package org.janelia.waves.thickness.opinion.weights;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.janelia.models.FunctionFitModel;
import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.functions.symmetric.SymmetricDifferentiableParameterizedFunction;

public class FunctionFitRansacWeightGenerator extends FitWeightGenerator {
	
	private final int nIterations;
	private final double threshold;
	
	public FunctionFitRansacWeightGenerator(CurveFitter fitter,
			SymmetricDifferentiableParameterizedFunction func,
			double[] initialGuess,
			int nIterations,
			double threshold) {
		super(fitter, func, initialGuess);
		this.nIterations = nIterations;
		this.threshold    = threshold;
	}

	public void generate(double[] coordinates, double[] measurements, long x, long y,
			double zRef, long zMin, long zMax, long zPosition) {
		
		this.initialGuess[0] = zRef;
		
		double[] weights = new double[ coordinates.length ];
		
		for ( int i = 0; i < measurements.length; ++i ) {
			weights[i] = this.getWeightFor( x, y, zMin + i);
		}
		
		
		
		this.fitter.clearObservations();
		
		
		ArrayList<PointMatch> inliers      = new ArrayList<PointMatch>();
		ArrayList<PointMatch> pointMatches = new ArrayList<PointMatch>();
		
		
		for (int i = 0; i < measurements.length; ++i ) {
			pointMatches.add( new PointMatch(new Point( new float[] { (float) coordinates[i] }), new Point( new float[] { (float) measurements[i] }), (float) this.getWeightFor( x, y, zMin + i ) ) );
		}
			
		
			
//		for ( int iteration = 0; iteration < nIterations && currentChange > threshold; ++iteration ) {
			
		FunctionFitModel model = new FunctionFitModel( this.initialGuess, func, fitter);
		
		inliers.clear();
		
		boolean ransacSuccessful = false;
		boolean filterSuccessful = false;
		
		try {
			ransacSuccessful = model.filterRansac( pointMatches, inliers, this.nIterations, (float)this.threshold, 0.5f);
		} catch (NotEnoughDataPointsException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			throw new RuntimeException( e2.getMessage() );
		}
		
		if ( ! ransacSuccessful ) {
			return;
		}
		
		this.initialGuess = model.getParameters();
		
		try {
			filterSuccessful = model.filter(pointMatches, inliers);
		} catch (NotEnoughDataPointsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException( e1.getMessage() );
		}
		
		if ( ! filterSuccessful ) {
			return;
		}
		
		this.initialGuess = model.getParameters();
		
		
//			System.out.print( "iteration " + iteration + ": ");
		
		for ( int i = 0; i < measurements.length; ++i ) {

			long currPos = i + zMin;
			PointMatch pm = pointMatches.get( i );
			
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
			
			if ( inliers.contains( pm ) ) {
				opinion.put( currPos, 0.0 );
			} else {
				opinion.put( currPos, 1.0 );
			}
			
			

			
		}
//			System.out.println();
//		}
	}
	

}
