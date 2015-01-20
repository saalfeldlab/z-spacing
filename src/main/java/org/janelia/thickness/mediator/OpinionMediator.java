package org.janelia.thickness.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.tuple.ConstantPair;

public interface OpinionMediator {
	
	public ArrayImg< DoubleType, DoubleArray > mediate( TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > shifts );
	
	public ArrayImg< DoubleType, DoubleArray > mediate( TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > shifts, ArrayImg< DoubleType, DoubleArray > result );
	
	public OpinionMediator copy();

}
