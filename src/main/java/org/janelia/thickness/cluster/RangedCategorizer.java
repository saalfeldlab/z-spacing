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
	private double[][] labels = null;
	private int numberOfSections = 1;
	
	/**
	 * @param windowRange
	 */
	public RangedCategorizer(final int windowRange) {
		super();
		this.windowRange = windowRange;
	}

	@Override
	public double[][] getLabels( final double[] coordinates ) {
		
		if ( this.labels == null || coordinates.length != this.labels.length )
			this.generateLabels( coordinates.length );
		return this.labels;
	}
	
	
	public void generateLabels( final int length ) {
		final int diameter = Math.min( windowRange, length );
		this.numberOfSections = (int) Math.round( length * 1.0 / diameter );
		final int correctedDiameter = (int) (length * 1.0 / numberOfSections);
		final double[][] labels = new double[ length ][ this.numberOfSections ];
		for (int i = 0; i < labels.length; i++) {
			final double currentSectionDouble = i * 1.0 /correctedDiameter;
			final int currentSectionFloor = (int) Math.floor( currentSectionDouble );
			final int currentSectionCeil  = currentSectionFloor + 1;
			
			final double r1 = currentSectionCeil   - currentSectionDouble;
			final double r2 = 1 - r1;
			
			if ( currentSectionCeil == this.numberOfSections )
				labels[i][ currentSectionFloor ] = 1.0;
			else {
				labels[i][ currentSectionFloor ] = r1;
				labels[i][ currentSectionCeil  ] = r2;
			}
		}
		this.labels = labels;
	}

}
