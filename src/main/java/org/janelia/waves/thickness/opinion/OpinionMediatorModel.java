package org.janelia.waves.thickness.opinion;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.models.TranslationModel1D;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantTriple;

public class OpinionMediatorModel implements OpinionMediatorInterface {
	
	private final double lambda;
	private final TreeMap< ConstantTriple<Long, Long, Long>, Tile< TranslationModel1D > > tileMap;
	private final TreeMap< ConstantTriple<Long, Long, Long>, Double> shiftMap;
	
	
	public OpinionMediatorModel(double lambda) {
		this( lambda, 
				new TreeMap<ConstantTriple<Long, Long, Long>, Tile< TranslationModel1D > >());
	}

	public OpinionMediatorModel(double lambda,
			TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> tileMap) {
		super();
		this.lambda = lambda;
		this.tileMap = tileMap;
		this.shiftMap = new TreeMap< ConstantTriple<Long, Long, Long>, Double>();
		
		for ( Entry<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> entry : tileMap.entrySet() ) {
			this.shiftMap.put( entry.getKey(), (double) entry.getValue().getModel().apply( new float[]{0.0f} )[0] );
		}
	}
	
	public void clearOpinions() {
		for ( Entry<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> entry : this.tileMap.entrySet() ) {
			entry.getValue().getMatches().clear();
		}
	}

	public void addOpinions( long x, long y, long zMin, long zMax, long zReference, double[] opinions, double[] weights ) throws InconsistencyError {
		if ( zMax <= zMin ) {
			throw new InconsistencyError();
		}
		
		if ( zReference < zMin || zReference > zMax ) {
			throw new InconsistencyError();
		}
		
		if ( opinions.length != zMax - zMin ) {
			throw new InconsistencyError();
		}
		
		if ( opinions.length != weights.length ) {
			throw new InconsistencyError();
		}
		
		for ( int i = 0; i < opinions.length; ++i ) {
			ConstantTriple<Long, Long, Long> position = new ConstantTriple<Long, Long, Long>(x, y, zMin );
			if ( ! this.tileMap.containsKey( position ) ) {
				TranslationModel1D model = new TranslationModel1D();
				this.tileMap.put( position, new Tile<TranslationModel1D>( model ) );
			}
			
			Tile<TranslationModel1D> tile = this.tileMap.get( position );
			
			// apply shift to p1 (which is at 0), thus strength must be 1.0f
			Point p1 = new Point( new float[] { 0.0f } );
			Point p2 = new Point( new float[] { (float) opinions[i] } );
			tile.addMatch( new PointMatch(p1, p2, (float) weights[i], (float) this.lambda ) ); 
			// 
			
			++zMin;
		}
		
		// update zMin ( used as indicator for zCurrent )
		
	}
	
	public void addOpinions( long x, long y, long zMin, long zMax, long zReference, double[] opinions ) throws InconsistencyError {
		long zReferenceShifted = zReference - zMin;
		double[] weights = new double[opinions.length];
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = 1.0 / ( 1 + Math.abs( i - zReferenceShifted ) );
		}
		this.addOpinions(x, y, zMin, zMax, zReference, opinions, weights);
	}
	
	public TreeMap<ConstantTriple<Long, Long, Long>, Double> fit() throws InterruptedException {
		
		return this.fit( Runtime.getRuntime().availableProcessors() );
		
	}
	
	
	
	
	public TreeMap<ConstantTriple<Long, Long, Long>, Double> fit( int nCores ) throws InterruptedException {
		
		if ( nCores <= 0 ) {
			nCores = Runtime.getRuntime().availableProcessors();
		}
		
		ExecutorService es = Executors.newFixedThreadPool( nCores );
		
		ArrayList< Callable<Void>> callables = new ArrayList<Callable<Void>>();
		
		for ( final Entry<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> entry : this.tileMap.entrySet() ) {
			
			callables.add( new Callable<Void>() {

				public Void call() throws Exception {
					entry.getValue().fitModel();
					return null;
				}
				
			});
			
		}
		
		es.invokeAll( callables );
		
		for ( Entry<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> entry : tileMap.entrySet() ) {
			this.shiftMap.put( entry.getKey(), (double) entry.getValue().getModel().apply( new float[]{0.0f} )[0] );
		}
		
		return this.shiftMap;
	}
	
	
}



























