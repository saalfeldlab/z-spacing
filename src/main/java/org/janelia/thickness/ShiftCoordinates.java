package org.janelia.thickness;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.LUTRealTransform;

import java.util.ArrayList;
import java.util.TreeMap;

public class ShiftCoordinates {
	
	public static < T extends RealType< T > > TreeMap< Long, ArrayList<ValuePair<Double, Double>> > collectShiftsFromMatrix(
			final double[] coordinates, 
			final RandomAccessibleInterval< T > correlations, 
			final double[] weights,
			final double[] multipliers,
			final ListImg< double[] > localFits,
			Options options ) {
		
		final RandomAccess< T > corrAccess1 = correlations.randomAccess();
		final RandomAccess< T > corrAccess2 = correlations.randomAccess();
		
		final TreeMap<Long, ArrayList<ValuePair<Double, Double> > > weightedShifts = new TreeMap< Long, ArrayList< ValuePair<Double, Double> > >();
		
		final double[] reference = new double[ 1 ];
		
		final ListCursor<double[]> cursor = localFits.cursor();
		
		
		// i is reference index, k is comparison index
		long width = correlations.dimension(0);
		long height = correlations.dimension(1);
		for ( int i = 0; i < height; ++i ) {
			
			corrAccess1.setPosition( i, 1 );
			corrAccess2.setPosition( i, 1 );
			final double[] localFit = cursor.next();
			final LUTRealTransform lut = new LUTRealTransform( localFit, 1, 1 );

			double minMeasurement1 = Double.MAX_VALUE;
			double minMeasurement2 = Double.MAX_VALUE;

			int startDist = 0; // TODO start at 1?
			// start up at i + startDist + 1 to avoid using diagonal twice
			for ( int dist = startDist, up = i + startDist + 1, down = i - startDist;
				  dist <= options.comparisonRange;
				  ++dist, ++up, --down )
			{

				if ( up < width ) {
					corrAccess1.setPosition(up, 0);

					double measurement = corrAccess1.get().getRealDouble();
					if ( Double.isNaN(measurement) || measurement <= options.minimumCorrelationValue || ( options.forceMonotonicity && measurement >= minMeasurement1 ) )
					{

					} else {

						minMeasurement1 = measurement;
						ArrayList<ValuePair<Double, Double>> localShifts = weightedShifts.get((long) up);
						if (localShifts == null) {
							localShifts = new ArrayList<ValuePair<Double, Double>>();
							weightedShifts.put((long) up, localShifts);
						}

						/* TODO inverts because LUTRealTransform can only increasing */
						reference[0] = -measurement;

						lut.applyInverse(reference, reference);

						if (!(reference[0] == Double.MAX_VALUE || reference[0] == -Double.MAX_VALUE)) {

							// rel: negative coordinates of k wrt to local coordinate system of i
							final double rel = coordinates[i] - coordinates[up];

							/* current location */
							final double shift = (up < i) ? rel - reference[0] : rel + reference[0];
							localShifts.add(new ValuePair<Double, Double>(shift, weights[i] * weights[up]));
						}
					}
				}

				if ( down >= 0 )
				{
					corrAccess2.setPosition(down, 0);

					double measurement = corrAccess2.get().getRealDouble();
					if ( Double.isNaN(measurement) || measurement <= options.minimumCorrelationValue || ( options.forceMonotonicity && measurement >= minMeasurement2) )
					{

					} else {

						minMeasurement2 = measurement;
						ArrayList<ValuePair<Double, Double>> localShifts = weightedShifts.get((long) down);
						if (localShifts == null) {
							localShifts = new ArrayList<ValuePair<Double, Double>>();
							weightedShifts.put((long) down, localShifts);
						}

						/* TODO inverts because LUTRealTransform can only increasing */
						reference[0] = -measurement;

						lut.applyInverse(reference, reference);

						if (!(reference[0] == Double.MAX_VALUE || reference[0] == -Double.MAX_VALUE)) {

							// rel: negative coordinates of k wrt to local coordinate system of i
							final double rel = coordinates[i] - coordinates[down];
							/* current location */
							final double shift = (down < i) ? rel - reference[0] : rel + reference[0];
							localShifts.add(new ValuePair<Double, Double>(shift, weights[i] * weights[down]));
						}
					}
				}
			}
		}
		return weightedShifts;
	}

}
