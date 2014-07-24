package org.janelia.waves.thickness.v2;

import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;

public class ResampleAtGrid {
	
	public ArrayImg< DoubleType, DoubleArray> resample( ArrayImg< DoubleType, DoubleArray> coordinates, 
			ArrayImg< DoubleType, DoubleArray> correlations, 
			InterpolatorFactory< DoubleType, RealRandomAccessible< DoubleType > > interpolatorFactory ) {
		
		final long MIN_Z = 0;
		final long MAX_Z = coordinates.dimension( 0 );
		
		final long N_CORRLEATION_SAMPLING_POINTS = correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS );
		final long N_CORRELATIONS_HALF           = N_CORRLEATION_SAMPLING_POINTS / 2;
		
		for ( int z = 0; z < correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ); ++z ) {
			
			long zLow  = Math.max( z - N_CORRELATIONS_HALF, MIN_Z );
			long zHigh = Math.min( z + N_CORRELATIONS_HALF, MAX_Z );
			
			
			
		}
		
		return correlations;
	}

}
