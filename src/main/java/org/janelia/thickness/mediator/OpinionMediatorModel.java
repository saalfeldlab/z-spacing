package org.janelia.thickness.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.tuple.ConstantPair;

public class OpinionMediatorModel< M extends Model<M> > implements OpinionMediator {
	
	private final M model;

	public OpinionMediatorModel(final M model) {
		super();
		this.model = model;
	}

	@Override
	public ArrayImg<DoubleType, DoubleArray> mediate(
			final TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts) {
		
		final double[] result = new double[ shifts.size() ];
		
		
		
		return mediate( shifts, result );
	}

	@Override
	public ArrayImg<DoubleType, DoubleArray> mediate(
			final TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts,
			final double[]result) {
		
		{
			for ( int i = 0; i < result.length; ++i ) {
				final ArrayList<ConstantPair<Double, Double>> localShifts = shifts.get( (long) i );
				final ArrayList<PointMatch> pointMatches = new ArrayList< PointMatch >();
				
				if ( localShifts == null || localShifts.size() == 0 )
					result[ i ] = 0.0;
				else {
					for ( final ConstantPair<Double, Double> l : localShifts ) {
						if ( Double.isInfinite( l.getA() ) || Double.isNaN( l.getA() ) ) {
							continue;
						}
						pointMatches.add( new PointMatch( new Point( new float[] { 0.0f } ), new Point( new float[] { new Float( l.getA() ) } ), new Float( l.getB() ) ) );
					}
					
//					final M mc = model.copy();
					try {
						model.fit( pointMatches );
						result[ i ] = model.apply( new float[] { 0.0f } )[ 0 ];
					} catch (final NotEnoughDataPointsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final IllDefinedDataPointsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
		
		return null;
	}
	
	@Override
	public OpinionMediatorModel< M > copy() {
		return new OpinionMediatorModel< M >( this.model.copy() );
	}

}
