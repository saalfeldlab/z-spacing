package org.janelia.correlations.pyramid;

import java.util.List;

import org.janelia.correlations.CorrelationsObjectInterface;


public class CorrelationsObjectPyramid {
	
	private final int numberOfLevels;
	private final int maxLevel;
	private final List< CorrelationsObjectInterface > pyramid;
	
	CorrelationsObjectPyramid( final List< CorrelationsObjectInterface > pyramid ) {
		this.numberOfLevels = pyramid.size();
		this.maxLevel       = numberOfLevels - 1;
		this.pyramid        = pyramid;
	}
	
	public CorrelationsObjectInterface get( final int index ) {
		return this.pyramid.get( index );
	}

	/**
	 * @return the numberOfLevels
	 */
	public int getNumberOfLevels() {
		return numberOfLevels;
	}

	/**
	 * @return the maxLevel
	 */
	public int getMaxLevel() {
		return maxLevel;
	}

}
