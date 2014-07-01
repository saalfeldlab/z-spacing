package org.janelia.waves.thickness.opinion.symmetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.janelia.exception.InconsistencyError;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.ConstantTriple;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface;
import org.janelia.waves.thickness.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.waves.thickness.opinion.Opinion;
import org.janelia.waves.thickness.opinion.OpinionFactory;
import org.janelia.waves.thickness.opinion.weights.DataBasedWeightGenerator;

/**
 * 
 * Collect correlations from a CorrelationsObjectInterface and shift z-coordinates to estimate thickness.
 * 
 * @author Philipp Hanslovsky
 *
 */
public class SymmetryAxisOpinionCollector {
	
	
	private final OpinionFactory opinionFactory;
	private final TreeMap< ConstantPair<Long, Long>, double[]> coordinates;
	private final CorrelationsObjectInterface correlations;
	private final ArrayList<Double> coordinateBase;
	private final DataBasedWeightGenerator weightGenerator;
	
	private final TreeMap<ConstantTriple<Long, Long, Long>, ConstantPair<Opinion, Meta> > opinions;
	
	private final int zBinMinimum;
	private final int zBinMaximum;
	
	private final SymmetryShiftRegularizer shiftRegularizer;
	
	
	
	/**
	 * @param opinionFactory     {@link OpinionFactory} that creates {@link Opinion} for the shift at this particular z bin
	 * @param coordinates        map containing list of (possibly shifted coordinates) for each xy position (if not shifted, all values should be the same)
	 * @param correlations       {@link CorrelationsObjectInterface} that holds cross correlations and meta data for each z slice
	 * @param coordinateBase     if {@link coordinates} does not contain key xy, coordinateBase will be put to xy
	 * @param weightGenerator    {@link DataBasedWeightGenerator} for fits
	 * @param opinions           possibly preoccupied opinion map (if it does not contain key xy, opinionFactory will be used) 
	 * @param zBinMinimum        minimum bin in z direction
	 * @param zBinMaximum        maximum bin in z direction
	 * @param shiftRegularizer   {@link SymmetryShiftRegularizer} that determines how to regularize and apply shifts
	 */
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			TreeMap<ConstantPair<Long, Long>, double[]> coordinates,
			CorrelationsObjectInterface correlations,
			ArrayList<Double> coordinateBase,
			DataBasedWeightGenerator weightGenerator,
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
	
	
	/**
	 * @param opinionFactory     {@link OpinionFactory} that creates {@link Opinion} for the shift at this particular z bin
	 * @param correlations       {@link CorrelationsObjectInterface} that holds cross correlations and meta data for each z slice
	 * @param weightGenerator    {@link DataBasedWeightGenerator} for fits
	 * @param zBinMinimum        minimum bin in z direction
	 * @param zBinMaximum        maximum bin in z direction
	 * @param shiftRegularizer   {@link SymmetryShiftRegularizer} that determines how to regularize and apply shifts
	 */
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			DataBasedWeightGenerator weightGenerator,
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
	
	
	/**
	 * @param opinionFactory     {@link OpinionFactory} that creates {@link Opinion} for the shift at this particular z bin
	 * @param correlations       {@link CorrelationsObjectInterface} that holds cross correlations and meta data for each z slice
	 * @param coordinateBase     if {@link coordinates} does not contain key xy, coordinateBase will be put to xy
	 * @param weightGenerator    {@link DataBasedWeightGenerator} for fits
	 * @param zBinMinimum        minimum bin in z direction
	 * @param zBinMaximum        maximum bin in z direction
	 * @param shiftRegularizer   {@link SymmetryShiftRegularizer} that determines how to regularize and apply shifts
	 */
	public SymmetryAxisOpinionCollector(
			OpinionFactory opinionFactory,
			CorrelationsObjectInterface correlations,
			ArrayList<Double> coordinateBase,
			DataBasedWeightGenerator weightGenerator,
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


	
	
	
	
	
	
	
	/**
	 * @author Philipp Hanslovsky
	 * Visitor interface for observing what is going on in @{link {@link SymmetryAxisOpinionCollector#shiftCoordinates(TreeMap, double, int, int, double, Visitor)}.
	 *
	 */
	public interface Visitor {
		void act( TreeMap< ConstantPair<Long, Long>, double[]> coordinates, int iteration, double change );
	}
	
	
	
	/**
	 * @param positions    z coordinates at x and y positions that should be shifted
	 * @param lambda       
	 * @param nCores       specify number of cores/threads
	 * @param nIterations  stop after nIterations
	 * @param threshold    stop if change is less than threshold
	 * @param visitor      observe what is going on
	 * @return the shifted coordinates
	 * @throws InconsistencyError
	 * @throws InterruptedException
	 */
	public TreeMap< ConstantPair<Long, Long>, double[]>  shiftCoordinates( final TreeMap< ConstantPair<Long, Long>, ArrayList<Long> > positions, 
			double lambda, int nCores, int nIterations, 
			double threshold, Visitor visitor ) throws InconsistencyError, InterruptedException {
		
		
		HashMap<Long, Meta> metaMap = this.correlations.getMetaMap();
		double change = Double.MAX_VALUE;
		
		for ( int iteration = 0; iteration < nIterations; ++iteration ) {
			
			
			
			// set change to 0.0 and also count the number of involved points; stop if change <= threshold
			change = 0.0;
			long nPoints = 0;
			
			// go to each xy position
			for (  Entry<ConstantPair<Long, Long>, ArrayList<Long>> xy : positions.entrySet() ) {
				
				
				// get zAxis at xy position, and generate it if null (not initialized yet)
				double[] zAxis  = this.coordinates.get( xy.getKey() );
				
				if ( zAxis == null ) {
					zAxis = new double[ this.zBinMaximum - this.zBinMinimum ];
					for ( int idx = 0; idx < zAxis.length; ++idx ) {
						
						zAxis[idx] = this.coordinateBase.get( idx );
						
					}
					this.coordinates.put( xy.getKey(), zAxis );
				}
				
				// generate weights
				// specify both the overall range of coordinates as well as the range of coordinates that we look at
				// also needs xy information for caching
				// range is set to -1 so that the range is determined by the function
				System.out.println( iteration + ":\n\t" + Arrays.toString( zAxis ) );
				this.weightGenerator.generateAtXY( zAxis, 
						                           this.correlations, 
						                           this.zBinMinimum, 
						                           this.zBinMaximum, 
						                           this.zBinMinimum, 
						                           this.zBinMaximum, 
						                           -1, 
						                           xy.getKey().getA(), 
						                           xy.getKey().getB() 
						                           );
				double[] weightsPrint = new double[ zAxis.length ];
				
				for ( int index = 0; index < weightsPrint.length; ++ index ) {
					weightsPrint[index] = this.weightGenerator.getWeightFor( xy.getKey().getA(), xy.getKey().getB(), index + this.zBinMinimum );
				}
				System.out.println( "\t" + Arrays.toString( weightsPrint) );
				
				double[] shifts = new double[ zAxis.length ]; // initialized to zero by default: http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
				
				for ( Long zBin : xy.getValue() ) {
					
					Meta meta = metaMap.get( zBin );
					
					// center z bin indices at this.zBinMinimum
					int localReferenceBin     = (int) meta.zPosition - this.zBinMinimum;
					
					// get subarray for local coordinates ( unfortunately, no views possible, thus copy neccessary )
					double[] localCoordinates = new double[ (int) (meta.zCoordinateMax - meta.zCoordinateMin) ];
					for ( int z = 0; z < localCoordinates.length; ++z ) {
						localCoordinates[z] = zAxis[ (int) (z + meta.zCoordinateMin - this.zBinMinimum) ];
					}
					
					
					// get previous opinion for this xyz position, initialize if not present yet
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
					
					// use opinion to get shift for the current z bin
					double[] weights = new double[ localCoordinates.length ];
					System.out.print( "\t\t" );
					ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> c = this.correlations.extractDoubleCorrelationsAt(xy.getKey().getA(), xy.getKey().getB(), zBin);
					Cursor<DoubleType> cursor = Views.iterable( c.getA() ).cursor();
					for ( int idx = 0; idx < localCoordinates.length; ++idx ) {
						weights[idx] = this.weightGenerator.getWeightFor( xy.getKey().getA(), xy.getKey().getB(), meta.zCoordinateMin + idx );

						System.out.print( "(b=" + ( meta.zCoordinateMin + idx ) + "," + String.format("w=%.3f,z=%.3f,c=%.3f) ", weights[idx], localCoordinates[idx], cursor.next().getRealDouble() ) );
					}
					System.out.println();
//					System.out.println( meta.zPosition );
//					for ( int i = 0; i < localCoordinates.length; ++ i) {
//						System.out.print( String.format( "%.03f", localCoordinates[i] ) + "\t");
//					}
//					System.out.println();
					
//					for ( double w : weights ) {
//						System.out.print( String.format( "%.03f\t", w ) );
//					}
//					System.out.println();
//					System.out.println();
					
					shifts[ localReferenceBin ] = opinion.getA().express( localCoordinates, weights )[0];
					nPoints += 1;
				}
				
				// extract weights for xy position
				double[] weights = new double[ shifts.length ];
				for ( int idx = 0; idx < shifts.length; ++idx ) {
//					weights[idx] = this.weightGenerator.getWeightFor( xy.getKey().getA(), xy.getKey().getB(), this.zBinMinimum + idx );
					weights[idx] = 1.0;
				}
				
				// apply shifts and sum up the change
				change += this.shiftRegularizer.shiftAndRegularize( zAxis, shifts, weights );
				
				
			}
			
			change /= nPoints;
			
			// do something, e.g. print the coordinates
			visitor.act( this.coordinates, iteration, change );
			if ( change < threshold ) {
				break;
			}
		}
		
		return this.coordinates;
		
	}

}
