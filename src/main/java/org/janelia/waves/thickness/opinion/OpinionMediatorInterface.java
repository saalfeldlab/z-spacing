package org.janelia.waves.thickness.opinion;

import java.util.TreeMap;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantTriple;

public interface OpinionMediatorInterface {
	
	public void clearOpinions();
	
	public void addOpinions( long x, long y, long zMin, long zMax, long zReference, double[] opinions, double[] weights ) throws InconsistencyError;
	
	public void addOpinions( long x, long y, long zMin, long zMax, long zReference, double[] opinions ) throws InconsistencyError;
	
	public TreeMap< ConstantTriple<Long, Long, Long>, Double > fit() throws InterruptedException;
	
	public TreeMap< ConstantTriple<Long, Long, Long>, Double > fit( int nCores ) throws InterruptedException;
}
