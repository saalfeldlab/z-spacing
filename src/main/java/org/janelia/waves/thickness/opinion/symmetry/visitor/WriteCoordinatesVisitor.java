package org.janelia.waves.thickness.opinion.symmetry.visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.janelia.utility.ConstantPair;
import org.janelia.waves.thickness.opinion.symmetry.SymmetryAxisOpinionCollector.Visitor;

public class WriteCoordinatesVisitor implements Visitor {
	
	final private String fileNameBase;

	public WriteCoordinatesVisitor(String fileNameBase) {
		super();
		this.fileNameBase = fileNameBase;
	}

	public void act(
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			int iteration, double change) {
		
		
		for ( Entry<ConstantPair<Long, Long>, double[]> xy : coordinates.entrySet() ) {
			
			
			try {
				Files.write( Paths.get( String.format( this.fileNameBase, iteration ) ), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String writeString = ""; // String.format( "bin,x=%d_y=%d,\n", xy.getKey().getA(), xy.getKey().getB() );
			
			for ( double c : xy.getValue() ) {
				writeString += "" + c + "\n";
			}
		
			try {
				Files.write( Paths.get( String.format( this.fileNameBase, iteration ) ), writeString.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit( 1 );
			}
			// TODO Auto-generated method stub
		}
	}
	
}
