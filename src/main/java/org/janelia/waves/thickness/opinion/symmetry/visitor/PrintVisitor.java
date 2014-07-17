package org.janelia.waves.thickness.opinion.symmetry.visitor;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;

public class PrintVisitor implements Visitor {
	
	private int iteration;
	private double change;

	public void act(
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			int iteration, double change) {
		for ( Entry<ConstantPair<Long, Long>, double[]> entry : coordinates.entrySet() ) {

			System.out.print( iteration + " " + change + "\t[" );
			for ( double e : entry.getValue() ) {
				System.out.print(e + "\t");
			}
			System.out.println("\b]");
			
			this.iteration = iteration;
			this.change    = change;
		}
		
	}
	
	public String toString() {
		return String.format( "iteration=%d, change=%f", this.iteration, this.change );
	}
	
}