package org.janelia.thickness;

import java.util.ArrayList;
import java.util.TreeMap;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.utility.ConstantPair;

public class ShiftCoordinates {
	
	public static TreeMap< Long, ArrayList< ConstantPair<Double, Double> > > collectShiftsFromMatrix(
			final double[] coordinates, 
			final ArrayImg< DoubleType, DoubleArray > correlations, 
			final double[] weights,
			final double[] multipliers,
			final LUTRealTransform lut ) {
		
		final ArrayRandomAccess<DoubleType> corrAccess = correlations.randomAccess();
		
		final TreeMap<Long, ArrayList<ConstantPair<Double, Double> > > weightedShifts = new TreeMap< Long, ArrayList< ConstantPair<Double, Double> > >();
		
		final double[] reference = new double[ 1 ];
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			corrAccess.setPosition( i, 1 );
			
			for ( int k = 0; k < correlations.dimension( 0 ); ++k ) {
				
				corrAccess.setPosition( k, 0 );
				
				if ( Double.isNaN( corrAccess.get().getRealDouble() ) )
					continue;
				
				ArrayList< ConstantPair< Double, Double > > localShifts = weightedShifts.get( ( long ) k );
				if ( localShifts == null ) {
					localShifts = new ArrayList<ConstantPair<Double,Double>>();
					weightedShifts.put( (long) k, localShifts );
				}
				
				final double m = ( k == i ) ? 1.0 : multipliers[ i ];
				
				/* TODO inverts because LUTRealTransform can only increasing */
				reference[ 0 ] = -corrAccess.get().get() * m;
				
				lut.applyInverse( reference, reference );
				
				if ( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE )
					continue;
				
				final double rel = coordinates[ i ] - coordinates[ k ];
				
				/* current location */
				final double shift = ( k < i ) ? rel - reference[ 0 ] : rel + reference[ 0 ];
				
				localShifts.add( new ConstantPair<Double, Double>( shift, weights[ i ] ) );
				
			}
		}
		return weightedShifts;
	}
	
	
	public static TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > collectShiftsFromMatrix(
			final RandomAccessibleInterval< DoubleType > localCoordinates,
			final RandomAccessibleInterval< DoubleType > localMatrices,
			final RandomAccessibleInterval< DoubleType > localWeights,
			final double[] multipliers,
			final LUTRealTransform lut,
			final int x,
			final int y
			) {
		
		final RandomAccess<DoubleType> matrixAccess     = localMatrices.randomAccess();
		final RandomAccess<DoubleType> coordinateAccess = localCoordinates.randomAccess();
		final RandomAccess<DoubleType> weightAccess     = localWeights.randomAccess();
		
		final TreeMap<Long, ArrayList<ConstantPair<Double, Double> > > weightedShifts = 
				new TreeMap< Long, ArrayList< ConstantPair<Double, Double> > >();
		final double[] reference = new double[ 1 ];
		
		for ( int i = 0; i < localMatrices.dimension( 1 ); ++i ) {
			matrixAccess.setPosition( i, 1 );
			
			coordinateAccess.setPosition( i, 0 );
			final double ci = coordinateAccess.get().get();
			
			weightAccess.setPosition( i,  0 );
			
			for ( int k = 0; k < localMatrices.dimension( 0 ); ++k ) {
				
				matrixAccess.setPosition( k, 0 );
				
				coordinateAccess.setPosition( k, 0 );
				final double ck = coordinateAccess.get().get();
			
				final double mValue = matrixAccess.get().get();
				if ( Double.isNaN( mValue) )
					continue;
				
				ArrayList< ConstantPair< Double, Double > > localShifts = weightedShifts.get( ( long ) k );
				if ( localShifts == null ) {
					localShifts = new ArrayList<ConstantPair<Double,Double>>();
					weightedShifts.put( (long) k, localShifts );
				}
				
				final double mult = ( k == i ) ? 1.0 : multipliers[ i ];
				/* TODO inverts because LUTRealTransform can only increasing */
				reference[ 0 ] = -mValue * mult;
				
				lut.applyInverse( reference, reference );
				if ( Math.abs( k - i ) < 4 ) {
//					System.out.println( reference[0] );
				}
				
				if ( reference[ 0 ] == Double.MAX_VALUE || reference[ 0 ] == -Double.MAX_VALUE ) {
					if ( Math.abs( k - i ) < 4 ) {
//						System.exit( 124353124 );
					}
					continue;
				}
				
				final double rel = ci - ck;
				
				/* current location */
				final double shift = ( k < i ) ? rel - reference[ 0 ] : rel + reference[ 0 ];
				
				localShifts.add( new ConstantPair<Double, Double>( shift, weightAccess.get().get() ) );
			}
			
		}

		return weightedShifts;
		
	}
	


}
