package org.janelia.waves.thickness.shift;

import org.janelia.exception.InconsistencyError;
import org.janelia.exception.ShiftEstimationException;

public interface ShiftEstimate {
	
	public double estimateShift( double[] coordinates, double[] measurements, double[] weights, double referenceCoordinate ) throws InconsistencyError, ShiftEstimationException;

}
