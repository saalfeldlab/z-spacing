package org.janelia.thickness;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.TranslationModel1D;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.thickness.lut.LUTRealTransform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
public class EstimateCorrelationsAtSamplePointsTest {
	
	private final int nCorrs = 100;
	private final int nRel   = 7;
	private final long seed  = 100;
	private final double sigma = 2.0;
	
	private final double[] weightArr = new double[ nCorrs ];
	
	private final ArrayImg<DoubleType, DoubleArray> cImage = ArrayImgs.doubles( nCorrs, nCorrs );
	private final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( weightArr, nCorrs );

	@Before
	public void setUp() throws Exception {
		
		for ( final DoubleType c : cImage ) {
			c.set( Double.NaN );
		}
		
		for ( final DoubleType w : weights ) {
			w.set( 1.0 );
		}
		
		final ArrayRandomAccess<DoubleType> cAccess = cImage.randomAccess();
		
		
		for ( int i = 0; i < nCorrs; ++ i) {
			cAccess.setPosition( i, 0 );
			for ( int j = -nRel; j <= nRel; ++j ) {
				if ( i + j < 0 || i + j >= nCorrs ) {
					continue;
				}
				cAccess.setPosition( i + j, 1 );
				cAccess.get().set( Math.exp( -0.5*j*j / Math.pow( sigma, 2.0 ) ) );
			}
		}
	}

	@Test
	public void test() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final double[] coordinates = new double[ nCorrs ];
		final double[] variances   = new double[ nCorrs ];
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i] = i;
		}
		final LUTRealTransform transform = new LUTRealTransform(coordinates, 2, 2);
		
		final double[] estimate = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( cImage, weightArr, transform, coordinates, nRel, new TranslationModel1D(), variances);
		
		int j = 0;
		for ( final double e : estimate ) {
			/* TODO inverts because LUTRealTransform can only increasing */
			Assert.assertEquals( -e, Math.exp( -0.5*j*j / Math.pow( sigma, 2.0 ) ), 0.00001 );
			++j;
		}
		
	}

}
