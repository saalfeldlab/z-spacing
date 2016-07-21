package org.janelia.thickness.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.util.ValuePair;

public interface OpinionMediator
{

	public void mediate( TreeMap< Long, ArrayList< ValuePair< Double, Double > > > shifts, double[] result );

	public OpinionMediator copy();

}
