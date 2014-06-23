package org.janelia.waves.thickness.opinion;

import org.janelia.waves.thickness.CorrelationsObjectInterface.Meta;

public interface OpinionFactory {

	public Opinion create( final double[] coordinates, final double[] measurements, Meta meta, double reference );
	
}
