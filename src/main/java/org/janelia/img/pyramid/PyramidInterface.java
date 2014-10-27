package org.janelia.img.pyramid;

import net.imglib2.RandomAccessibleInterval;

public interface PyramidInterface< T > {
	
	public RandomAccessibleInterval< T > get( int level );
	
	public RandomAccessibleInterval< T > get( double level );
	
	public int getMaxLevel();

}
