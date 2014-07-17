package org.janelia.waves.thickness.v2;

import java.util.ArrayList;
import java.util.TreeMap;

import org.janelia.utility.ConstantPair;

import mpicbg.models.Model;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

public class ShiftCoordinates {
	
	public static TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > collectShifts( ArrayImg< DoubleType, DoubleArray > coordinates, 
			ArrayImg< DoubleType, DoubleArray > correlations, 
			ArrayImg< DoubleType, DoubleArray > weights, 
			RealRandomAccessible< DoubleType > correlationFit,
			RealRandomAccessible< DoubleType > correlationFitGradient ) {
		
		TreeMap<Long, ArrayList<ConstantPair<Double, Double> > > weightedShifts = new TreeMap< Long, ArrayList< ConstantPair<Double, Double> > >();
		
		ArrayRandomAccess<DoubleType> coordinateRandomAccess   = coordinates.randomAccess();
		ArrayRandomAccess<DoubleType> correlationsRandomAccess = correlations.randomAccess();
		ArrayRandomAccess<DoubleType> weightRandomAccess       = weights.randomAccess();
		RealRandomAccess<DoubleType> fitRandomAccess           = correlationFit.realRandomAccess();
		RealRandomAccess<DoubleType> gradientRandomAccess      = correlationFitGradient.realRandomAccess();
		
		for ( int z = 0; z < correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ); ++z ) {
			
			correlationsRandomAccess.setPosition( z, CorrelationsObjectToArrayImg.Z_AXIS );
			
			for ( int dz = 0; dz < correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ); ++ dz ) {
				
				int shiftedDz = dz - (int) correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) / 2;
				int currentZ  = z + shiftedDz;
				
				coordinateRandomAccess.setPosition( 0, currentZ );
				correlationsRandomAccess.setPosition( dz, CorrelationsObjectToArrayImg.DZ_AXIS );
				weightRandomAccess.setPosition( 0, currentZ );
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get(), 0 );
				gradientRandomAccess.setPosition( coordinateRandomAccess.get().get(), 0 );
				
				if ( Double.isNaN( correlationsRandomAccess.get().get() ) ) {
					continue;
				}
				
				ArrayList<ConstantPair<Double, Double>> localShifts = weightedShifts.get( currentZ );
				if ( localShifts == null ) {
					localShifts = new ArrayList<ConstantPair<Double,Double>>();
					weightedShifts.put( (long) currentZ, localShifts );
				}
				
				
				
//				localShifts.add( 
//								findShiftForDataPoint( coordinateRandomAccess.get().get(), 
//										               correlationsRandomAccess.get().get(),
//										               weightRandomAccess.get().get(),
//										               currentZ, 
//										               correlationFit )
//								);
				double difference = correlationsRandomAccess.get().get() - fitRandomAccess.get().get();
				localShifts.add( new ConstantPair< Double, Double >( difference / gradientRandomAccess.get().get() , weightRandomAccess.get().get() ) );
				
			}
			
		}
		
		
		return weightedShifts;
	}
	
	public static TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > collectShiftsFromMatrix( ArrayImg< DoubleType, DoubleArray > coordinates, 
			ArrayImg< DoubleType, DoubleArray > correlations, 
			ArrayImg< DoubleType, DoubleArray > weights,
			ArrayImg< DoubleType, DoubleArray > multipliers,
			RealRandomAccessible< DoubleType > correlationFit,
			RealRandomAccessible< DoubleType > correlationFitGradient ) {
		
		ArrayRandomAccess<DoubleType> corrAccess    = correlations.randomAccess();
		ArrayRandomAccess<DoubleType> coordAccess   = coordinates.randomAccess();
		RealRandomAccess<DoubleType> fitAccess      = correlationFit.realRandomAccess();
		RealRandomAccess<DoubleType> gradientAccess = correlationFitGradient.realRandomAccess();
		ArrayRandomAccess<DoubleType> weightAccess  = weights.randomAccess();
		ArrayRandomAccess<DoubleType> multAccess    = multipliers.randomAccess();
		
		TreeMap<Long, ArrayList<ConstantPair<Double, Double> > > weightedShifts = new TreeMap< Long, ArrayList< ConstantPair<Double, Double> > >();
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			corrAccess.setPosition( i, 1 );
			multAccess.setPosition( i, 0 );
			
			coordAccess.setPosition( i, 0 );
			double zRef = coordAccess.get().get();
			
			
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
				
//				double difference = corrAccess.get().get() * multAccess.get().get() - fitAccess.get().get();
				double difference = corrAccess.get().get() * 1.0 - fitAccess.get().get();
				weightAccess.setPosition( k, 0 );
				
//				if ( Math.abs( gradientAccess.get().getRealDouble() ) > 0.0 ) 
					localShifts.add( new ConstantPair<Double, Double>( difference / gradientAccess.get().getRealDouble(), weightAccess.get().getRealDouble() ) );
				
			}
		}
		return weightedShifts;
	}
	


}
