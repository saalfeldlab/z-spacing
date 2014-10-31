/**
 * 
 */
package org.janelia.thickness.cluster;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class RangedCategorizer implements Categorizer {

	private final int windowRange;
	private int[] labels = null;
	
	/**
	 * @param windowRange
	 */
	public RangedCategorizer(final int windowRange) {
		super();
		this.windowRange = windowRange;
	}

	@Override
	public int[] getLabels( final double[] coordinates ) {
		
		if ( this.labels == null || coordinates.length != this.labels.length )
			this.generateLabels( coordinates.length );
		return this.labels;
	}
	
	
	protected void generateLabels( final int length ) {
		final int diameter = Math.min( windowRange, length );
		final long numberOfSections = Math.round( length * 1.0 / diameter );
		final int correctedDiameter = (int) (length * 1.0 / numberOfSections);
		final int[] labels = new int[ length ];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = i /correctedDiameter;
		}
		this.labels = labels;
	}

}
