package org.janelia.thickness;

import java.util.ArrayList;
import java.util.TreeMap;

import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.LUTRealTransform;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * 
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ShiftCoordinates
{

	public static < T extends RealType< T > > TreeMap< Long, ArrayList< Double > > collectShiftsFromMatrix(
			final double[] coordinates,
			final RandomAccessibleInterval< T > correlations,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > localFits,
			Options options )
	{

		final RandomAccess< T > corrAccess1 = correlations.randomAccess();
		final RandomAccess< T > corrAccess2 = correlations.randomAccess();

		final TreeMap< Long, ArrayList< Double > > weightedShifts = new TreeMap< Long, ArrayList< Double > >();

		final double[] reference = new double[ 1 ];

		final Cursor< double[] > cursor = Views.iterable( localFits ).cursor();

		// i is reference index, k is comparison index
		long width = correlations.dimension( 0 );
		long height = correlations.dimension( 1 );
		for ( int i = 0; i < height; ++i )
		{

			corrAccess1.setPosition( i, 1 );
			corrAccess2.setPosition( i, 1 );
			final double[] localFit = cursor.next();
			final LUTRealTransform lut = new LUTRealTransform( localFit, 1, 1 );

			double minMeasurement1 = Double.MAX_VALUE;
			double minMeasurement2 = Double.MAX_VALUE;

			int startDist = 0; // TODO start at 1?
			// start up at i + startDist + 1 to avoid using diagonal twice
			for ( int dist = startDist, up = i + startDist + 1, down = i - startDist; dist <= options.comparisonRange; ++dist, ++up, --down )
			{

				if ( up < width )
				{
					corrAccess1.setPosition( up, 0 );

					double measurement = corrAccess1.get().getRealDouble();
					if ( Double.isNaN( measurement ) || measurement <= options.minimumCorrelationValue || ( options.forceMonotonicity && measurement >= minMeasurement1 ) )
					{

					}
					else
					{

						minMeasurement1 = measurement;
						ArrayList< Double > localShifts = weightedShifts.get( ( long ) up );
						if ( localShifts == null )
						{
							localShifts = new ArrayList< Double >();
							weightedShifts.put( ( long ) up, localShifts );
						}

						/*
						 * TODO inverts because LUTRealTransform can only
						 * increasing
						 */
						reference[ 0 ] = -measurement;

						lut.applyInverse( reference, reference );

						// reference[0] > halfRange || ??
						if ( !( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE ) )
						{

							// rel: negative coordinates of k wrt to local
							// coordinate system of i
							final double rel = coordinates[ i ] - coordinates[ up ];

							/* current location */
							final double shift = ( up < i ) ? rel - reference[ 0 ] : rel + reference[ 0 ];
							localShifts.add( shift );
						}
					}
				}

				if ( down >= 0 )
				{
					corrAccess2.setPosition( down, 0 );

					double measurement = corrAccess2.get().getRealDouble();
					if ( Double.isNaN( measurement ) || measurement <= options.minimumCorrelationValue || ( options.forceMonotonicity && measurement >= minMeasurement2 ) )
					{

					}
					else
					{

						minMeasurement2 = measurement;
						ArrayList< Double > localShifts = weightedShifts.get( ( long ) down );
						if ( localShifts == null )
						{
							localShifts = new ArrayList< Double >();
							weightedShifts.put( ( long ) down, localShifts );
						}

						/*
						 * TODO inverts because LUTRealTransform can only
						 * increasing
						 */
						reference[ 0 ] = -measurement;

						lut.applyInverse( reference, reference );

						// reference[0] > halfRange || ??
						if ( !( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE ) )
						{

							// rel: negative coordinates of k wrt to local
							// coordinate system of i
							final double rel = coordinates[ i ] - coordinates[ down ];
							/* current location */
							final double shift = ( down < i ) ? rel - reference[ 0 ] : rel + reference[ 0 ];
							localShifts.add( shift );
						}
					}
				}
			}
		}
		return weightedShifts;
	}

}
