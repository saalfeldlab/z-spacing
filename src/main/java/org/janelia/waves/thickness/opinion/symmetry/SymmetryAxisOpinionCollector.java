package org.janelia.waves.thickness.opinion.symmetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.exception.InconsistencyError;
import org.janelia.waves.thickness.ConstantPair;
import org.janelia.waves.thickness.ConstantTriple;
import org.janelia.waves.thickness.CorrelationsObjectInterface;
import org.janelia.waves.thickness.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.opinion.Opinion;
import org.janelia.waves.thickness.opinion.OpinionFactory;
import org.janelia.waves.thickness.opinion.WeightGenerator;

public class SymmetryAxisOpinionCollector {
	
	
	private final OpinionFactory opinionFactory;
	private final TreeMap< ConstantPair<Long, Long>, double[]> coordinates;
	private final CorrelationsObjectInterface correlations;
	private final ArrayList<Double> coordinateBase;
	private final WeightGenerator weightGenerator;
	
	private final TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta> > opinions;
	
	private final int zBinMinimum;
	private final int zBinMaximum;
	
	private final SymmetryShiftRegularizer shiftRegularizer;
	
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			CorrelationsObjectInterface correlations,
			ArrayList<Double> coordinateBase,
			WeightGenerator weightGenerator,
			TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta>> opinions,
			int zBinMinimum, int zBinMaximum,
			SymmetryShiftRegularizer shiftRegularizer) {
		super();
		this.opinionFactory = opinionFactory;
		this.coordinates = coordinates;
		this.correlations = correlations;
		this.coordinateBase = coordinateBase;
		this.weightGenerator = weightGenerator;
		this.opinions = opinions;
		this.zBinMinimum = zBinMinimum;
		this.zBinMaximum = zBinMaximum;
		this.shiftRegularizer = shiftRegularizer;
	}
	
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			WeightGenerator weightGenerator,
			int zBinMinimum, int zBinMaximum,
			SymmetryShiftRegularizer shiftRegularizer) {
		super();
		this.opinionFactory = opinionFactory;
		this.coordinates = new TreeMap<ConstantPair<Long, Long>, double[]>();
		this.correlations = correlations;
		this.coordinateBase = new ArrayList<Double>();
		this.weightGenerator = weightGenerator;
		this.opinions = new TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta>>();
		this.zBinMinimum = zBinMinimum;
		this.zBinMaximum = zBinMaximum;
		this.shiftRegularizer = shiftRegularizer;
		
		for ( int i = zBinMinimum; i < zBinMaximum; ++i ) {
			this.coordinateBase.add( (double) i );
		}
		
	}
	
	
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			ArrayList<Double> coordinateBase,
			WeightGenerator weightGenerator,
			int zBinMinimum, int zBinMaximum,
			SymmetryShiftRegularizer shiftRegularizer) {
		super();
		this.opinionFactory = opinionFactory;
		this.coordinates = new TreeMap<ConstantPair<Long, Long>, double[]>();
		this.correlations = correlations;
		this.coordinateBase = coordinateBase;
		this.weightGenerator = weightGenerator;
		this.opinions = new TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta>>();
		this.zBinMinimum = zBinMinimum;
		this.zBinMaximum = zBinMaximum;
		this.shiftRegularizer = shiftRegularizer;
		
		for ( int i = zBinMinimum; i < zBinMaximum; ++i ) {
			this.coordinateBase.add( (double) i );
		}
		
	}


	
	
	
	
	
	
	
	public interface Visitor {
		void act( TreeMap< ConstantPair<Long, Long>, double[]> coordinates, int iteration, double change );
	}
	
	public TreeMap< ConstantPair<Long, Long>, double[]>  shiftCoordinates( final TreeMap< ConstantPair<Long, Long>, ArrayList<Long> > positions, 
			double lambda, int nCores, int nIterations, 
			double threshold, Visitor visitor ) throws InconsistencyError, InterruptedException {
		
		this.opinions.clear();
		
		HashMap<Long, Meta> metaMap = this.correlations.getMetaMap();
		
		double change = Double.MAX_VALUE;
		
		for ( int iteration = 0; iteration < nIterations && change > threshold; ++iteration ) {
			
			
			change = 0.0;
			
			long nPoints = 0;
			
			for (  Entry<ConstantPair<Long, Long>, ArrayList<Long>> xy : positions.entrySet() ) {
				
				double[] zAxis  = this.coordinates.get( xy.getKey() );
				
				
				if ( zAxis == null ) {
					zAxis = new double[ this.zBinMaximum - this.zBinMinimum ];
					for ( int idx = 0; idx < zAxis.length; ++idx ) {
						
						zAxis[idx] = this.coordinateBase.get( idx );
						
					}
					this.coordinates.put( xy.getKey(), zAxis );
				}
				
				double[] shifts = new double[ zAxis.length ]; // initialized to zero by default: http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
				
				for ( Long zBin : xy.getValue() ) {
					
					Meta meta = metaMap.get( zBin );
					
					int localReferenceBin     = (int) meta.zPosition - this.zBinMinimum;
					double[] localCoordinates = new double[ (int) (meta.zCoordinateMax - meta.zCoordinateMin) ];
					
					for ( int z = 0; z < localCoordinates.length; ++ z ) {
						localCoordinates[z] = zAxis[ (int) (z + meta.zCoordinateMin - this.zBinMinimum) ];
					}
					
					
					ConstantPair<Opinion, Meta> opinion = this.opinions.get( new ConstantTriple<Long, Long, Long>(xy.getKey().getA(), xy.getKey().getB(), zBin ) );
					
					if ( opinion == null ) {
						double[] measurements = new double[ localCoordinates.length ];
						ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> c = this.correlations.extractDoubleCorrelationsAt(xy.getKey().getA(), xy.getKey().getB(), zBin);
						Cursor<DoubleType> cursor = Views.iterable( c.getA() ).cursor();
						for ( int idx = 0; idx < measurements.length; ++ idx ) {
							measurements[idx] = cursor.next().getRealDouble();
						}
						opinion = new ConstantPair<Opinion, Meta>( this.opinionFactory.create( localCoordinates, measurements, meta, localCoordinates[ (int) (meta.zPosition - meta.zCoordinateMin) ]), meta );
						this.opinions.put( new ConstantTriple<Long, Long, Long>(xy.getKey().getA(), xy.getKey().getB(), zBin ), opinion );
					}
					
					shifts[ localReferenceBin ] = opinion.getA().express( localCoordinates )[0];
					
					nPoints += 1;
				}
				
				double[] weights = new double[ shifts.length ];
				for ( int idx = 0; idx < shifts.length; ++idx ) {
					weights[idx] = 1.0;
				}
				

				change += this.shiftRegularizer.shiftAndRegularize( zAxis, shifts, weights );
				
				
			}
			
			change /= nPoints;
			
			
			visitor.act( this.coordinates, iteration, change );
		}
		
		return this.coordinates;
		
	}

}
