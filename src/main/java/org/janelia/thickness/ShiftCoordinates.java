package org.janelia.thickness;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.ConstantPair;

public class ShiftCoordinates {
	
	public static TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > collectShiftsFromMatrix( final ArrayImg< DoubleType, DoubleArray > coordinates, 
			final ArrayImg< DoubleType, DoubleArray > correlations, 
			final ArrayImg< DoubleType, DoubleArray > weights,
			final ArrayImg< DoubleType, DoubleArray > multipliers,
			final RealRandomAccessible< DoubleType > correlationFit,
			final RealRandomAccessible< DoubleType > correlationFitGradient ) {
		
		final ArrayRandomAccess<DoubleType> corrAccess    = correlations.randomAccess();
		final ArrayRandomAccess<DoubleType> coordAccess   = coordinates.randomAccess();
		final RealRandomAccess<DoubleType> fitAccess      = correlationFit.realRandomAccess();
		final RealRandomAccess<DoubleType> gradientAccess = correlationFitGradient.realRandomAccess();
		final ArrayRandomAccess<DoubleType> weightAccess  = weights.randomAccess();
		final ArrayRandomAccess<DoubleType> multAccess    = multipliers.randomAccess();
		
		final TreeMap<Long, ArrayList<ConstantPair<Double, Double> > > weightedShifts = new TreeMap< Long, ArrayList< ConstantPair<Double, Double> > >();
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			corrAccess.setPosition( i, 1 );
			multAccess.setPosition( i, 0 );
			
			coordAccess.setPosition( i, 0 );
			final double zRef = coordAccess.get().get();
			
			
			for ( int k = 0; k < correlations.dimension( 0 ); ++k ) {
				
				corrAccess.setPosition( k, 0 );
				coordAccess.setPosition( k, 0 );
				
				if ( Double.isNaN( corrAccess.get().getRealDouble() ) ) {
					continue;
				}
				
				
				
				fitAccess.setPosition( coordAccess.get().getRealDouble() - zRef, 0 );
				if ( Double.isNaN( fitAccess.get().getRealDouble() ) ) {
					continue;
				}
				
				gradientAccess.setPosition( coordAccess.get().getRealDouble() - zRef, 0 );
				if ( Double.isNaN( gradientAccess.get().getRealDouble() ) ) {
					continue;
				}
				
				ArrayList<ConstantPair<Double, Double>> localShifts = weightedShifts.get( (long) k );
				if ( localShifts == null ) {
					localShifts = new ArrayList<ConstantPair<Double,Double>>();
					weightedShifts.put( (long) k, localShifts );
				}
				
				
				final double m = ( k == i ) ? 1.0 : multAccess.get().get();
				
				final double difference = corrAccess.get().get() * m - fitAccess.get().get();
				weightAccess.setPosition( k, 0 );
				
				localShifts.add( new ConstantPair<Double, Double>( difference / gradientAccess.get().getRealDouble(), weightAccess.get().getRealDouble() ) );
				
			}
		}
		return weightedShifts;
	}
	


}
