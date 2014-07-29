package org.janelia.thickness;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.models.ScaleModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EstimateQualityOfSliceTest {
	
	private final int nData = 100;
	private final int correlationRange = 7;
	private final double sigma = 2.0;
	private final ArrayImg< DoubleType, DoubleArray > matrix  = ArrayImgs.doubles( nData, nData );
	private final ArrayImg< DoubleType, DoubleArray > coord   = ArrayImgs.doubles( nData );
	private final ArrayImg< DoubleType, DoubleArray > weights = ArrayImgs.doubles( nData );
	private final ArrayImg< DoubleType, DoubleArray > fit     = ArrayImgs.doubles( correlationRange );

	@Before
	public void setUp() throws Exception {
		
		for ( final DoubleType w : weights ) {
			w.set( 1.0 );
		}
		
		for ( final DoubleType m : matrix ) {
			m.set( Double.NaN );
		}
		
		int k = 0;
		for ( final DoubleType f : fit ) {
			f.set( Math.exp( -0.5*k*k / Math.pow( sigma, 2.0 ) ) );
			++k;
		}
		
		final ArrayCursor<DoubleType> coordCursor        = coord.cursor();
		final ArrayRandomAccess<DoubleType> matrixAccess = matrix.randomAccess();
		
		for ( int i = 0; i < nData; ++i ) {
			coordCursor.next().set( i );
			matrixAccess.setPosition( i, 0 );
			for ( int j = -correlationRange; j <= correlationRange; ++j ) {
				if ( i + j < 0 || i + j >= nData ) {
					continue;
				}
				matrixAccess.setPosition( i + j, 1 );
				matrixAccess.get().set( Math.exp( -0.5*j*j / Math.pow( sigma, 2.0 ) ) * 1.0 / ( i + 1 ) );
			}
		}
		
	}

	@Test
	public void test() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		final FitWithGradient fitWithGradient = new FitWithGradient( fit, new FitWithGradient.SymmetricGradient(), new NearestNeighborInterpolatorFactory<DoubleType>() );
		
		final double regularizerWeight = 0.0;
		final ArrayImg<DoubleType, DoubleArray> multipilers = EstimateQualityOfSlice.estimateFromMatrix( matrix, weights, new ScaleModel(), coord, fitWithGradient.getFit(), 1, regularizerWeight );
		int k = 1;
		for ( final DoubleType m : multipilers ) {
			Assert.assertEquals( ( 1.0 - regularizerWeight ) * k + regularizerWeight, m.getRealDouble(), 0.0001);
			++k;
		}
	}

}
