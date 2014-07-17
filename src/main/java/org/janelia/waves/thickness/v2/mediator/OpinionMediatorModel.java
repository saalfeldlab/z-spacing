package org.janelia.waves.thickness.v2.mediator;

import java.util.ArrayList;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;

import org.janelia.utility.ConstantPair;

public class OpinionMediatorModel< M extends Model<M> > implements OpinionMediator {
	
	private final M model;

	public OpinionMediatorModel(M model) {
		super();
		this.model = model;
	}

	public ArrayImg<DoubleType, DoubleArray> mediate(
			TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts) {
		
		ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( new double[ shifts.size() ], shifts.size() );
		
		
		
		return mediate( shifts, result );
	}

	public ArrayImg<DoubleType, DoubleArray> mediate(
			TreeMap<Long, ArrayList<ConstantPair<Double, Double>>> shifts,
			ArrayImg<DoubleType, DoubleArray> result) {
		
		{
			ArrayCursor<DoubleType> cursor = result.cursor();
			for ( int i = 0; i < result.dimension( 0 ); ++i ) {
				cursor.fwd();
				ArrayList<ConstantPair<Double, Double>> localShifts = shifts.get( (long) i );
				ArrayList<PointMatch> pointMatches = new ArrayList< PointMatch >();
				
				if ( localShifts == null ) {
					cursor.get().set( 0.0 );
				}
				
				else {
					System.out.println( localShifts.size() );
					for ( ConstantPair<Double, Double> l : localShifts ) {
						if ( Double.isInfinite( l.getA() ) || Double.isNaN( l.getA() ) ) {
							continue;
						}
						pointMatches.add( new PointMatch( new Point( new float[] { 0.0f } ), new Point( new float[] { new Float( l.getA() ) } ), new Float( l.getB() ) ) );
					}
					
					try {
						model.fit( pointMatches );
					} catch (NotEnoughDataPointsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllDefinedDataPointsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cursor.get().set( model.apply( new float[] { 0.0f } )[ 0 ] );
				}
			}
		}
		
		return null;
	}

}
