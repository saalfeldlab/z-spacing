package org.janelia.waves.thickness.opinion.symmetry.visitor;

import java.util.TreeMap;

import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;

public class LazyVisitor implements Visitor {
	
	private int iteration;
	private double change;

	public void act(
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			int iteration, double change) {
		this.iteration = iteration;
		this.change    = change;
	}
	
	public String toString() {
		return String.format( "iteration=%d, change=%f", this.iteration, this.change );
	}
	
}
