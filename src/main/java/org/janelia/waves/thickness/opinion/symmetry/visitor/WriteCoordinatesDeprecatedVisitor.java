package org.janelia.waves.thickness.opinion.symmetry.visitor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;

public class WriteCoordinatesDeprecatedVisitor implements Visitor {
	
	final private String fileNameBase;

	public WriteCoordinatesDeprecatedVisitor(String fileNameBase) {
		super();
		this.fileNameBase = fileNameBase;
	}

	public void act(
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			int iteration, double change) {
		
		
		for ( Entry<ConstantPair<Long, Long>, double[]> xy : coordinates.entrySet() ) {
			
			
			try {
				FileWriter file = new FileWriter( String.format( this.fileNameBase, iteration ) );
				String writeString = ""; // String.format( "bin,x=%d_y=%d,\n", xy.getKey().getA(), xy.getKey().getB() );
				for ( double c : xy.getValue() ) {
					writeString += "" + c + "\n";
				}
				file.write( writeString );
				file.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
}
