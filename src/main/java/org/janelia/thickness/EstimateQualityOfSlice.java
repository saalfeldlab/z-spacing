package org.janelia.thickness;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class EstimateQualityOfSlice {
	
	private final static float[] ONE_DIMENSION_ONE_POSITION = new float[] { 1.0f };

	public static < M extends Model< M >, C extends Model< C > > ArrayImg< DoubleType, DoubleArray > estimateFromMatrix(
			final ArrayImg< DoubleType, DoubleArray > correlations,
			final ArrayImg< DoubleType, DoubleArray > weights,
			final M model,
			final ArrayImg< DoubleType, DoubleArray > coordinates,
			final RealRandomAccessible< DoubleType > correlationFit,
			final int nThreads,
			final double regularizerWeight ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final ArrayImg<DoubleType, DoubleArray> multipliers         = ArrayImgs.doubles( weights.dimension( 0 ) );
		final ArrayCursor<DoubleType> multiplierCursor              = multipliers.cursor();
		final RealRandomAccess<DoubleType> fitRandomAccess         = correlationFit.realRandomAccess();
		final ArrayRandomAccess<DoubleType> coordinateRandomAccess = coordinates.randomAccess();
		final ArrayRandomAccess<DoubleType> weightRanodmAccess     = weights.randomAccess();
		
		final double inverseRegularizerWeight = 1 - regularizerWeight;
		
		for ( int z = 0; z < correlations.dimension( 0 ); ++z ) {
			
			final IterableInterval<DoubleType> correlationsAtBin = Views.flatIterable( Views.hyperSlice( correlations, 0, z ) );
			final ArrayList< PointMatch > pointMatches           = new ArrayList<PointMatch>();
			
			coordinateRandomAccess.setPosition( z, 0 );
			final double refCoordinate = coordinateRandomAccess.get().get();
			
			final Cursor< DoubleType > correlationBinCursor = correlationsAtBin.localizingCursor();
			while ( correlationBinCursor.hasNext() ) {
				
				final DoubleType tc = correlationBinCursor.next();
				final int currentZ = correlationBinCursor.getIntPosition( 0 );
				
				if ( currentZ == z )
					continue;
				
				final double c = tc.get();
				coordinateRandomAccess.setPosition( currentZ, 0);
				fitRandomAccess.setPosition( coordinateRandomAccess.get().get() - refCoordinate, 0 );
				weightRanodmAccess.setPosition( currentZ, 0 );

				final double fra = fitRandomAccess.get().get();
				
				if ( Double.isNaN( c ) || Double.isNaN( fra ) )
					continue;
				
				pointMatches.add(
						new PointMatch(
								new Point( new float[]{ ( float )c } ),
								new Point( new float[]{ ( float )fra } ),
								weightRanodmAccess.get().getRealFloat() ) );
				
			}
	
			model.fit( pointMatches );
			
			/* set factor regularized towards 1.0 */
			multiplierCursor.next().set( model.apply( ONE_DIMENSION_ONE_POSITION )[0] * inverseRegularizerWeight + regularizerWeight );
			
		}
		return multipliers;
	}
	
}
