package org.janelia.waves.thickness.opinion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import mpicbg.models.Tile;
import mpicbg.models.TranslationModel1D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.opinion.weights.WeightGenerator;


public class OpinionCollector {
	
	public class OpinionMeta {
		public long zMin;
		public long zMax;
		public long zReference;
		
		public OpinionMeta(long zMin, long zMax, long zReference) {
			super();
			this.zMin = zMin;
			this.zMax = zMax;
			this.zReference = zReference;
		}
		
		
	}
	
	
	public interface Visitor {
		void act( TreeMap< ConstantPair<Long, Long>, double[]> coordinates, int iteration, double change );
	}
	
	
	private final OpinionFactory opinionFactory;
	private final TreeMap< ConstantPair<Long, Long>, double[]> coordinates;
	private final CorrelationsObjectInterface correlations;
	private final ArrayList<Double> coordinateBase;
	private final WeightGenerator weightGenerator;
	
	private final TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta>> opinions;
	
	private final long zMinimum;
	private final long zMaximum;
	
	private final OpinionMediatorFactoryInterface mediatorFactory;
	
	private final double zeroThreshold;
	
	
	




	public OpinionCollector(OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			WeightGenerator weightGenerator,
			OpinionMediatorFactoryInterface mediatorFactory ) {
		this( opinionFactory, correlations, weightGenerator, null, mediatorFactory, 0.0001 );
	}
	
	
	public OpinionCollector(OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			WeightGenerator weightGenerator,
			ArrayList<Double> coordinateBase,
			OpinionMediatorFactoryInterface mediatorFactory,
			double zeroThreshold ) {
		super();
		this.opinionFactory = opinionFactory;
		this.coordinates    = new TreeMap< ConstantPair<Long, Long>, double[]>();
		this.correlations   = correlations;
		this.opinions       = new TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta>>();
		
		this.zMinimum        = this.correlations.getzMin();
		this.zMaximum        = this.correlations.getzMax();
		
		this.mediatorFactory = mediatorFactory;
		
		this.zeroThreshold = zeroThreshold;
		
		if ( coordinateBase == null ) {
		
			this.coordinateBase  = new ArrayList<Double>();
			for ( long i  = this.zMinimum; i < this.zMaximum; ++i) {
				this.coordinateBase.add( (double) ( i ) );
			}
			
		} else {
			this.coordinateBase = coordinateBase;
		}
		
		this.weightGenerator = weightGenerator;
			
		
		
		
	}


	/**
	 * @return the zMinimum
	 */
	public long getzMinimum() {
		return zMinimum;
	}


	/**
	 * @return the zMaximum
	 */
	public long getzMaximum() {
		return zMaximum;
	}


	public TreeMap< ConstantPair<Long, Long>, double[]> shiftCoordinates( final TreeMap< Long, ArrayList<ConstantPair<Long, Long> > > positions, double lambda, int nCores, int nIterations, double threshold, Visitor visitor ) throws InconsistencyError, InterruptedException {
		
		this.opinions.clear();
		
		HashMap<Long, Meta> metaMap = this.correlations.getMetaMap();
		
		OpinionMediatorInterface mediator = this.mediatorFactory.create( lambda );
		
		double change = Double.MAX_VALUE;
		
		for ( int iteration = 0; iteration < nIterations && change > threshold; ++iteration ) {
			
			mediator.clearOpinions();
			
			change = 0.0;
			
			long nPoints = 0;
			
			for ( Entry<Long, ArrayList<ConstantPair<Long, Long> > > entry : positions.entrySet() ) {
				Meta meta = metaMap.get( entry.getKey() );
				for ( ConstantPair<Long, Long> xy : entry.getValue() ) {
					ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> measurementCoordinatePair = this.correlations.extractDoubleCorrelationsAt( xy.getA(), xy.getB(), entry.getKey() );
					
					
					
					double[] coordinatesAt = this.coordinates.get( xy );
					double[] measurements  = new double[(int) measurementCoordinatePair.getA().dimension(0)];
					
					if( coordinatesAt == null ) {
						coordinatesAt  = new double[ this.coordinateBase.size() ];
						
						
						for ( int i = 0; i < coordinatesAt.length; ++i ) {
							coordinatesAt[i] = this.coordinateBase.get( i );
						}
						
						this.coordinates.put( xy, coordinatesAt );
					}
					
					{
						int i = 0;
						for (  DoubleType c : Views.flatIterable( measurementCoordinatePair.getA() ) ) {
							measurements[i] = c.getRealDouble();
							++i;
						}
					}
					
					double[] coord = Arrays.copyOfRange(coordinatesAt, (int) ( meta.zCoordinateMin - this.zMinimum ), (int) ( meta.zCoordinateMax - this.zMinimum ) );
					
					double reference = coord[ (int) (meta.zPosition - meta.zCoordinateMin) ];
					
					Opinion opinion  = this.opinionFactory.create( coord, measurements, meta, reference );
					double[] shifts  = opinion.express();
					double[] weights = this.weightGenerator.generate( meta.zCoordinateMin, meta.zCoordinateMax, meta.zPosition );
					
					mediator.addOpinions( xy.getA(), xy.getB(), meta.zCoordinateMin, meta.zCoordinateMax, meta.zPosition, shifts, weights);
					++nPoints;
				}
				
			}
			
			TreeMap<ConstantTriple<Long, Long, Long>, Double> mediatedFits = mediator.fit( nCores );
			
			for ( Entry<ConstantTriple<Long, Long, Long>, Double> entry : mediatedFits.entrySet() ) {
				double shift = entry.getValue(); // entry.getKey().getC() } )[0];
				if ( shift < this.zeroThreshold ) {
					shift = 0.0;
				}
				
				double[] tmp = this.coordinates.get( new ConstantPair<Long, Long>(entry.getKey().getA(), entry.getKey().getB() ) );
				tmp[(int) (entry.getKey().getC() - this.zMinimum)] = tmp[(int) (entry.getKey().getC() - this.zMinimum)] + shift;
				change += Math.abs( shift );
			}
			
			if ( nPoints > 0 ) {
				change /= nPoints;
			}
			
			visitor.act( this.coordinates, iteration, change );
		}
		
		return this.coordinates;
	}

	
	
	
	public TreeMap< ConstantPair<Long, Long>, double[]> shiftCoordinates( final TreeMap< Long, ArrayList<ConstantPair<Long, Long> > > positions, double lambda, int nCores, int nIterations, double threshold ) throws InconsistencyError, InterruptedException {
		return this.shiftCoordinates(positions, lambda, nCores, nIterations, threshold, new Visitor() {
			
			public void act(TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
					int iteration, double change) {
				// do nothing
			}
		});
	}
}
