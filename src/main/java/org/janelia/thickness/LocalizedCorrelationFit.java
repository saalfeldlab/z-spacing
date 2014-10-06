package org.janelia.thickness;

import java.util.ArrayList;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class LocalizedCorrelationFit {
	
	public static interface WeightGenerator {
		
		public double calculate( int c1, int c2);
		
		public float calculatFloat( int c1, int c2 );
		
	}
	
	private static final float[] ONE_DIMENSION_ZERO_POSITION = new float[]{ 0.0f };
	
	private final WeightGenerator weightGenerator;
	
	
	/**
	 * @param weightGenerator
	 */
	public LocalizedCorrelationFit(final WeightGenerator weightGenerator) {
		super();
		this.weightGenerator = weightGenerator;
	}


	public <M extends Model< M > > void estimateFromMatrix( final RandomAccessibleInterval< DoubleType > correlations,
			final double[] coordinates,
			final double[] weights,
			final double[] multipliers,
			final AbstractLUTRealTransform transform,
			final int range,
			final M correlationFitModel,
			final ListImg< double[] > localFits,
			final int windowRange) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		
		assert localFits.numDimensions() == 2;
		assert localFits.dimension( 1 ) == coordinates.length;
		assert localFits.firstElement().length == range;
		
		
		
		final ArrayList< ArrayList< ArrayList< PointMatch > > > pointCollections = new ArrayList< ArrayList<  ArrayList< PointMatch > > >();
		
		for ( int i = 0; i < coordinates.length; ++i ) {
			final ArrayList< ArrayList< PointMatch> > localArrayList = new ArrayList<ArrayList<PointMatch>>();
			for ( int k = 0; k <= range; ++k ) {
				localArrayList.add( new ArrayList<PointMatch>() );
			}
			pointCollections.add( localArrayList );
		}
		
		final RealRandomAccessible<DoubleType> source = Views.interpolate( Views.extendValue( correlations, new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>());
		
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> source2 = RealViews.transformReal(source, transform);
		final LUTRealTransform tf1d = new LUTRealTransform( coordinates, 1, 1);
		final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> multipliersInterpolatedTransformed = 
				RealViews.transformReal( Views.interpolate( Views.extendValue( ArrayImgs.doubles( multipliers, multipliers.length ), new DoubleType( Double.NaN ) ), new NLinearInterpolatorFactory<DoubleType>()), 
				tf1d );

		final RealRandomAccess< DoubleType > access  = source2.realRandomAccess();
		final RealRandomAccess< DoubleType > access2 = source2.realRandomAccess();
		
		final RealRandomAccess< DoubleType> m1 = multipliersInterpolatedTransformed.realRandomAccess();
		final RealRandomAccess< DoubleType> m2 = multipliersInterpolatedTransformed.realRandomAccess();
		
		for ( int i = 0; i < correlations.dimension( 1 ); ++i ) {
			
			access.setPosition( i, 1 );
			access.setPosition( i, 0 );
			
			transform.apply(access, access);
			access2.setPosition(access);
			
			m1.setPosition( i, 0 );
			tf1d.apply( m1, m1 );
			m2.setPosition( m1 );
			final double mref = m1.get().get();
			
			final int lower = Math.max( 0,  i - windowRange );
			final int upper = Math.min( coordinates.length, i + windowRange );
			
			for ( int k = 0; k <= range; ++k, access.fwd( 0 ), access2.bck( 0 ), m1.fwd( 0 ), m2.bck( 0 ) ) {
				
//				if ( i < coordinates.length - 1 && coordinates[i] + k < coordinates[ i + 1 ] )
//					continue;
//				
//				if ( i > 0 && coordinates[i] - k > coordinates[ i - 1 ] )
//					continue;
				
				final double a1 = access.get().get();
				final double a2 = access2.get().get();
				
				if ( ! Double.isNaN( a1 ) )
				{
					final int index = i + k;
					if ( index < weights.length ) {
						final float w1 = (float)weights[ index ]; // replace 1.0 by real weight, as soon as weight calculation has become clear 
	//					for ( int m = lower; m < upper; ++m ) {
	//						pointCollections.get( m ).get( k ).add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a1 } ) ) );// , (float) ( w1 * weightGenerator.calculate( i, m ) ) ) );
	//					}
						// no local fits momentarily, just use the same fit:
						final ArrayList<PointMatch> pC = pointCollections.get( 0 ).get( k );
						pC.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)( a1*mref*m1.get().get() ) } ), w1 ) );
					}
				}
				
				if ( ( ! Double.isNaN( a2 ) ) )
				{
					final int index = i - k;
					if ( index > 0 ) {
						final float w2 = (float)weights[ index ]; // replace 1.0 by real weight, as soon as weight calculation has become clear 
	//					for ( int m = lower; m < upper; ++m ) {
	//						pointCollections.get( m ).get( k ).add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)a2 } ) ) );// , (float) ( w2 * weightGenerator.calculate( i, m ) ) ) );
	//					}
						// no local fits momentarily, just use the same fit:
						final ArrayList<PointMatch> pC = pointCollections.get( 0 ).get( k );
						pC.add( new PointMatch( new Point( ONE_DIMENSION_ZERO_POSITION ), new Point( new float[]{ (float)( a2*mref*m2.get().get() ) } ), w2 ) );
					}
				}
				
				
				
			}
					
		}
		
		
		/* TODO inverts because LUTRealTransform can only increasing */
		final ListCursor<double[]> cursor = localFits.cursor();
		// no local fits momentarily, just use the same fit:
		final double[] lf = localFits.firstElement();
		lf[0] = -1;
		final ArrayList<ArrayList<PointMatch>> pC = pointCollections.get( 0 );
		for ( int k = 1; k < lf.length; ++k ) {
			correlationFitModel.fit( pC.get( k ) );
			lf[ k ] = -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
		}
		
		while ( cursor.hasNext() ) {
			cursor.fwd();
			cursor.set( lf );
		}
		
		// when back to local fits, use this:
//		for ( int m = 0; cursor.hasNext(); ++m ) {
//			
//			final double[] localFit = cursor.next();
//			localFit[0] = -1;
//			final ArrayList<ArrayList<PointMatch>> localPoints = pointCollections.get( m );
//			
//			for ( int k = 1; k < localFit.length; ++k ) {
//				correlationFitModel.fit( localPoints.get( k ) );
//				
//				
//				
//				/* TODO inverts because LUTRealTransform can only increasing */
//				localFit[ k ] = -correlationFitModel.apply( ONE_DIMENSION_ZERO_POSITION )[ 0 ];
//
//			}
//		}
		
	}

}
