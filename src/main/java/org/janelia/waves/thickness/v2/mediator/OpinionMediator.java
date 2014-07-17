package org.janelia.waves.thickness.v2.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import org.janelia.utility.ConstantPair;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

public interface OpinionMediator {
	
	public ArrayImg< DoubleType, DoubleArray > mediate( TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > shifts );
	
	public ArrayImg< DoubleType, DoubleArray > mediate( TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > shifts, ArrayImg< DoubleType, DoubleArray > result );

}
