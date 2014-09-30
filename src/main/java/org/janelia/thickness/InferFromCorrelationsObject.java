package org.janelia.thickness;

import ij.IJ;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import mpicbg.models.AffineModel1D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.thickness.LocalizedCorrelationFit.WeightGenerator;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.SingleDimensionLUTRealTransformField;
import org.janelia.thickness.lut.TransformRandomAccessibleInterval;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.ConstantPair;

public class InferFromCorrelationsObject< M extends Model<M>, L extends Model<L> > {

        private final CorrelationsObjectInterface correlationsObject;
        private final M correlationFitModel;
        private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory;
        private final L measurementsMultiplierModel;
        private final OpinionMediator shiftMediator;
        private final long zMin;
        private final long zMax;

        public static class Options {

                public static Options generateDefaultOptions() {
                        final Options result = new Options();
                        result.multiplierGenerationRegularizerWeight = 0.01;
                        result.coordinateUpdateRegularizerWeight = 0.01;
                        result.shiftProportion = 0.5;
                        result.nIterations = 1;
                        result.nThreads = 1;
                        result.comparisonRange = 10;
                        result.neighborRegularizerWeight = 0.05;
                        result.minimumSectionThickness = 0.1;
                        result.windowRange = 150;
                        result.shiftsSmoothingSigma = 4.0;
                        result.shiftsSmoothingRange = 10;
                        result.withRegularization = true;
                        return result;
                }

                public double multiplierGenerationRegularizerWeight; // m_regularized = m * ( 1 - w ) + 1 * w
                public double coordinateUpdateRegularizerWeight; // coordinate_regularized = predicted * ( 1 - w ) + original * w
                public double shiftProportion; // actual_shift = shift * shiftProportion
                public int nIterations; // number of iterations
                public int nThreads; // number of threads
                public int comparisonRange; // range for cross correlations
                public double neighborRegularizerWeight;
                public double minimumSectionThickness;
                public int windowRange;
                public double shiftsSmoothingSigma;
                public int shiftsSmoothingRange;
                public boolean withRegularization;
                
                @Override
				public String toString() {
                	final StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    sb.append(getClass().getName());
                    sb.append("]\n");
                    for ( final Field f : this.getClass().getDeclaredFields() ) {
                  	  sb.append( f.getName() );
                  	  sb.append( "\t" );
                  	  try {
          				final StringBuilder append = sb.append( f.get( this ) );
          			} catch (final IllegalArgumentException e) {
          				// TODO Auto-generated catch block
          				e.printStackTrace();
          			} catch (final IllegalAccessException e) {
          				// TODO Auto-generated catch block
          				e.printStackTrace();
          			}
                  	  sb.append( "\n" );
                    }
                    
                    return sb.toString();
                }


        }


        public InferFromCorrelationsObject(
                final CorrelationsObjectInterface correlationsObject,
                final M correlationFitModel,
                final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory,
                final L measurementsMultiplierModel,
                final OpinionMediator shiftMediator ) {
                super();

                this.correlationsObject = correlationsObject;
                this.correlationFitModel = correlationFitModel;
                this.fitInterpolatorFactory = fitInterpolatorFactory;
                this.measurementsMultiplierModel = measurementsMultiplierModel;
                this.shiftMediator = shiftMediator;

                final Iterator<Long> iterator = this.correlationsObject.getMetaMap().keySet().iterator();
                zMin = iterator.next();
                long zMaxTmp = zMin;

                while ( iterator.hasNext() )
                        zMaxTmp = iterator.next();
                zMax = zMaxTmp + 1;
               
        }


        public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates( final long x,
                                                                         final long y,
                                                                         final double[] startingCoordinates,
                                                                         final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
                return estimateZCoordinates( x, y, startingCoordinates, new Visitor() {

                        @Override
                        public void act(final int iteration,
                                        final ArrayImg<DoubleType, DoubleArray> matrix, final double[] lut,
                                        final AbstractLUTRealTransform transform,
                                        final double[] multipliers,
                                        final double[] weights,
                                        final double[] estimatedFit ) {
                                // don't do anything
                        }},
                        options );
        }


        private static RealRandomAccessible< DoubleType > mirrorAndExtend(
                final double[] data,
                final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory )
        {
                final ArrayImg< DoubleType, DoubleArray > img = ArrayImgs.doubles( data, new long[]{ data.length } );
                final RandomAccessible< DoubleType > mirror = Views.extendMirrorSingle( img );
                final RandomAccessibleInterval< DoubleType > crop = Views.interval( mirror, new long[]{ 1 - data.length }, new long[]{ data.length - 1 } );
                final RandomAccessible< DoubleType > extension = Views.extendValue( crop, new DoubleType( Double.NaN ) );
                return Views.interpolate( extension, interpolatorFactory );
        }


        public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates(
                final long x,
                final long y,
                final double[] startingCoordinates,
                final Visitor visitor,
                final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

                final ArrayImg<DoubleType, DoubleArray> matrix = this.correlationsToMatrix( x, y );

                final double[] weightArr = new double[ ( int )matrix.dimension( 0 ) ];
                final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( weightArr, new long[]{ weightArr.length } );

                for ( final DoubleType w : weights) {
                        w.set( 1.0 );
                }

                final double[] lut = startingCoordinates;
                final double[] coordinateArr = lut;


                final ArrayImg<DoubleType, DoubleArray> coordinates = ArrayImgs.doubles( coordinateArr, coordinateArr.length );


                final LUTRealTransform transform = new LUTRealTransform(lut, matrix.numDimensions(), matrix.numDimensions() );

                final ArrayImg<DoubleType, DoubleArray> mediatedShifts = ArrayImgs.doubles( lut.length );



                visitor.act( 0, matrix, lut, transform, weightArr, weightArr, new double[ lut.length ] );
                
                final double[] weightTable = new double[ lut.length ];
                for (int i = 0; i < weightTable.length; i++) {
					weightTable[ i ] = Math.exp( - 0.5*i*i / 40000 );
				}
                
                final WeightGenerator wg = new LocalizedCorrelationFit.WeightGenerator() {
                	
					
					@Override
					public double calculate(final int c1, final int c2) {
//						final int dist = Math.abs( c1 - c2 );
//						return dist >= weightTable.length ? 0.0 : weightTable[ dist ];
						return weightTable[ Math.abs( c1 - c2 ) ];
					}
					
					@Override
					public float calculatFloat(final int c1, final int c2) {
						return (float) calculate(c1, c2);
					}
				};
				final LocalizedCorrelationFit lcf = new LocalizedCorrelationFit( wg );
				final ArrayList<double[]> fitList = new ArrayList< double[] >();
				for ( int i = 0; i < lut.length; ++i ) {
					fitList.add( new double[ options.comparisonRange ] );
				}
				
				final ListImg< double[] > localFits = new ListImg<double[]>( fitList, fitList.size() );

                for ( int n = 0; n < options.nIterations; ++n ) {

                        this.iterationStep(matrix, weightArr, transform, lut, coordinateArr, coordinates, mediatedShifts, options, visitor, n, lcf, localFits );
                        IJ.showProgress( n, options.nIterations );
                }
                return coordinates;
        }

        public ArrayImg<DoubleType, DoubleArray> estimateZCoordinatesLocally(
                final int start,
                final int stop,
                final int step,
                final double[] defaultCoordinates,
                final Options options
                                                                             ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
                return estimateZCoordinatesLocally( start, stop, step, defaultCoordinates, new Visitor() {

                        @Override
                        public void act(final int iteration, final ArrayImg<DoubleType, DoubleArray> matrix,
                                        final double[] lut, final AbstractLUTRealTransform transform, final double[] multipliers,
                                        final double[] weights, final double[] estimatedFit) {
                                // do not do anything
                        }
                }, options );
        }


        public ArrayImg<DoubleType, DoubleArray> estimateZCoordinatesLocally(
                final int start,
                final int stop,
                final int step,
                final double[] defaultCoordinates,
                final Visitor visitor,
                final Options options
                                                                             ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
                return estimateZCoordinatesLocally( start, start, stop, stop, step, step, defaultCoordinates, visitor, options );
        }


        public ArrayImg<DoubleType, DoubleArray> estimateZCoordinatesLocally(
                final int startX,
                final int startY,
                final int stopX,
                final int stopY,
                final int stepX,
                final int stepY,
                final double[] defaultCoordinates,
                final Options options
                                                                             ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
                return estimateZCoordinatesLocally(startX, startY, stopX, stopY, stepX, stepY, defaultCoordinates, new Visitor() {

                        @Override
                        public void act(
                                final int iteration,
                                final ArrayImg<DoubleType, DoubleArray> matrix,
                                final double[] lut,
                                final AbstractLUTRealTransform transform,
                                final double[] multipliers,
                                final double[] weights,
                                final double[] estimatedFit) {
                                // do not do anything
                        }
                },
                        options);
        }


        public ArrayImg<DoubleType, DoubleArray> estimateZCoordinatesLocally(
                final int startX,
                final int startY,
                final int stopX,
                final int stopY,
                final int stepX,
                final int stepY,
                final double[] defaultCoordinates,
                final Visitor visitor,
                final Options options
                                                                             ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

                final int nX = ( stopX - startX ) / stepX;
                final int nY = ( stopY - startY ) / stepY;

                final double[] defaultWeights = new double[ defaultCoordinates.length ];
                for (int i = 0; i < defaultWeights.length; i++) {
                        defaultWeights[i] = 1.0;
                }
                
                final double[] arange = new double[ defaultCoordinates.length ];
                for (int i = 0; i < arange.length; i++) {
					arange[i] = i;
				}
                final AffineModel1D coordinatesFit = new AffineModel1D();


                final ArrayList<AbstractLUTRealTransform> transforms = new ArrayList<AbstractLUTRealTransform>();
                final TreeMap<ConstantPair< Integer, Integer >, double[] > coordinatesMap = new TreeMap<ConstantPair<Integer,Integer>, double[]>();
                final TreeMap<ConstantPair< Integer, Integer >, double[] > weightsMap = new TreeMap<ConstantPair<Integer,Integer>, double[]>();
                final TreeMap<ConstantPair< Integer, Integer >, ArrayImg<DoubleType, DoubleArray> > matrices = new TreeMap<ConstantPair<Integer,Integer>, ArrayImg<DoubleType,DoubleArray>>();
                final TreeMap<ConstantPair<Integer, Integer>, ArrayImg<DoubleType, DoubleArray>> mediatedShiftsMap = new TreeMap<ConstantPair<Integer, Integer>, ArrayImg<DoubleType, DoubleArray> >();

                int x = startX;
                int y = startY;

                for ( int ny = 0; ny < nY; ++ny ) {
                        x = startX;
                        for (int nx = 0; nx < nX; ++nx ) {
                                final ConstantPair<Integer, Integer> xy = new ConstantPair<Integer, Integer>( nx, ny );
                                final ArrayImg<DoubleType, DoubleArray> matrix = this.correlationsToMatrix( x, y );
                                matrices.put( xy, matrix );
                                final double[] localCoordinates = defaultCoordinates.clone();
                                transforms.add( new LUTRealTransform( localCoordinates, 2, 2 ) );
                                coordinatesMap.put( xy, localCoordinates );
                                weightsMap.put( xy, defaultWeights.clone() );
                                x += stepX;
                        }
                        y += stepY;
                }

                final TransformRandomAccessibleInterval transformsAccessible = new TransformRandomAccessibleInterval( new long[] { nX, nY }, transforms );
                final RandomAccess<AbstractLUTRealTransform> access = transformsAccessible.randomAccess();


                for ( int iteration = 0; iteration < options.nIterations; ++iteration )
                {
                        final TreeMap<ConstantPair<Integer, Integer>, double[]> previousCoordinates = new TreeMap<ConstantPair<Integer,Integer>, double[]>();
                        for ( final Entry<ConstantPair<Integer, Integer>, double[]> entry : coordinatesMap.entrySet() ) {
                                previousCoordinates.put( entry.getKey(), entry.getValue().clone() );
                        }
                        for ( int ny = 0; ny < nY; ++ny ) {
                                access.setPosition( ny, 1 );
                                for (int nx = 0; nx < nX; ++nx ) {
                                        access.setPosition( nx, 0 );
                                        final ConstantPair<Integer, Integer> xy = new ConstantPair<Integer, Integer>( nx, ny );
                                        final double[] c = coordinatesMap.get( xy );
                                        iterationStep(
                                                matrices.get( xy ),
                                                weightsMap.get( xy ),
                                                access.get(),
                                                c,
                                                c,
                                                ArrayImgs.doubles( c, c.length ),
                                                ArrayImgs.doubles( c.length ),
                                                options,
                                                visitor,
                                                iteration);
                                        final double[] neighbors = new double[ c.length ];
                                        int count = 0;
                                        for ( int dy = -1; dy < 2; ++dy ) {
                                                for ( int dx = -1; dx < 2; ++dx ) {
                                                        final int xPos = nx + dx;
                                                        final int yPos = ny + dy;
                                                        if ( ( dy == 0 && dx == 0 ) || xPos < 0 || yPos < 0 || xPos >= nX || yPos >= nY ) {
                                                                continue;
                                                        }
                                                        final double[] cNeighbor = previousCoordinates.get( new ConstantPair< Integer, Integer>( xPos, yPos ) );
                                                        for ( int z = 0; z < c.length; ++z ) {
                                                                neighbors[z] += cNeighbor[ z ];
                                                        }
                                                        ++count;
                                                }
                                        }
                                        final double ownWeight = 1.0 - options.neighborRegularizerWeight;
                                        for ( int z = 0; z < c.length; ++ z ) {
                                                c[z] = ownWeight*c[z] + options.neighborRegularizerWeight*neighbors[z] / count;
                                        }
                                }
                        }
                }



                final ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( nX, nY, defaultCoordinates.length );
                final ArrayCursor<DoubleType> resultCursor = result.cursor();
                while( resultCursor.hasNext() ) {
                        resultCursor.fwd();
                        resultCursor.get().set( coordinatesMap.get( new ConstantPair<Integer, Integer>( resultCursor.getIntPosition( 0 ), resultCursor.getIntPosition( 1 ) ) )[ resultCursor.getIntPosition( 2 ) ] ); // - resultCursor.getIntPosition( 2 )  );
                }

                return result;
        }


        private void iterationStep( final ArrayImg<DoubleType, DoubleArray> matrix,
                                    final double[] weights,
                                    final AbstractLUTRealTransform transform,
                                    final double[] lut,
                                    final double[] coordinateArr,
                                    final ArrayImg< DoubleType, DoubleArray > coordinates,
                                    final ArrayImg< DoubleType, DoubleArray > mediatedShifts,
                                    final Options options,
                                    final Visitor visitor,
                                    final int n
                                    ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
                final double[] vars = new double[ options.comparisonRange ];

                final double[] estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( matrix, weights, transform, coordinateArr, options.comparisonRange, this.correlationFitModel, vars );

                final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;

                final double[] multipliers = EstimateQualityOfSlice.estimateFromMatrix( matrix,
                                                                                        weights,
                                                                                        this.measurementsMultiplierModel,
                                                                                        coordinates,
                                                                                        mirrorAndExtend( estimatedFit, new NLinearInterpolatorFactory< DoubleType >() ),
                                                                                        options.nThreads,
                                                                                        options.multiplierGenerationRegularizerWeight );


                final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
                                ShiftCoordinates.collectShiftsFromMatrix(
                                        coordinateArr,
                                        matrix,
                                        weights,
                                        multipliers,
                                        new LUTRealTransform( estimatedFit, 1, 1 ) );

                this.shiftMediator.mediate( shifts, mediatedShifts );

                final ArrayCursor<DoubleType> mediatedCursor = mediatedShifts.cursor();
                
                double accumulatedShifts = 0.0;

                for ( int ijk = 0; mediatedCursor.hasNext(); ++ijk ) {

                        mediatedCursor.fwd();
                        final double previous = lut[ijk];
                        lut[ ijk ] += accumulatedShifts + options.shiftProportion * mediatedCursor.get().get();
                        // make sure, that slices do not flip positions, ie set lut[ijk] to
                        // previous + shiftProportion * ( lut[ijk-1] - previous )
                        if ( ijk > 0 && lut[ ijk ] < lut[ ijk - 1 ] ) {
                        	String logStr = "" + ijk + " " + lut[ ijk ] + " <= " + lut[ ijk - 1 ];
                        	lut[ ijk ] = lut[ ijk - 1 ] + ( lut[ ijk - 1 ] - lut[ ijk ] ) * ( options.shiftProportion );
                        	logStr += ", now: lut[ijk] = " + lut[ijk];
                        }
//                        lut[ijk] *= inverseCoordinateUpdateRegularizerWeight;
//                        lut[ijk] += options.coordinateUpdateRegularizerWeight * ijk;
                        accumulatedShifts = lut[ ijk ] - previous;
                }
                
                final float[] floatLut = new float[ lut.length ]; final float[] arange = new float[ lut.length ];
                final float[] floatWeights = new float[ lut.length ];
                for (int i = 0; i < arange.length; i++) {
					arange[i] = i;
					floatLut[i] = (float) lut[i];
					floatWeights[i] = 1.0f;
				}
                final AffineModel1D coordinatesFitModel = new AffineModel1D();
                
           
                coordinatesFitModel.fit( new float[][]{floatLut}, new float[][]{arange}, floatWeights );
                
                final double[] affineArray = new double[ 2 ];
                coordinatesFitModel.toArray( affineArray );
                
                for (int i = 0; i < lut.length; i++) {
					lut[i] = affineArray[0] * lut[i] + affineArray[1];
				}

                visitor.act( n + 1, matrix, lut, transform, multipliers, weights, estimatedFit );
        }
        
        
        private void iterationStep( final ArrayImg<DoubleType, DoubleArray> matrix,
                final double[] weights,
                final AbstractLUTRealTransform transform,
                final double[] lut,
                final double[] coordinateArr,
                final ArrayImg< DoubleType, DoubleArray > coordinates,
                final ArrayImg< DoubleType, DoubleArray > mediatedShifts,
                final Options options,
                final Visitor visitor,
                final int n,
                final LocalizedCorrelationFit lcf,
                final ListImg< double[] > localFits
                ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
			final double[] vars = new double[ options.comparisonRange ];
			
			lcf.estimateFromMatrix(matrix, coordinateArr, weights, transform, options.comparisonRange, correlationFitModel, localFits, options.windowRange);
//			}	
			
			final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;
			
			final double[] multipliers = EstimateQualityOfSlice.estimateFromMatrix( matrix,
			                                                                    weights,
			                                                                    this.measurementsMultiplierModel,
			                                                                    coordinates,
			                                                                    localFits, // mirrorAndExtend( estimatedFit, new NLinearInterpolatorFactory< DoubleType >() ),
			                                                                    options.nThreads,
			                                                                    options.multiplierGenerationRegularizerWeight );
			
			final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
			            ShiftCoordinates.collectShiftsFromMatrix(
			                    coordinateArr,
			                    matrix,
			                    weights,
			                    multipliers,
			                    localFits );
			
			this.shiftMediator.mediate( shifts, mediatedShifts );
			
			final ArrayCursor<DoubleType> mediatedCursor = mediatedShifts.cursor();
			
			double accumulatedShifts = 0.0;
			final double[] smoothedShifts = new double [ (int) mediatedShifts.dimension( 0 ) ];
			final double[] gaussKernel    = new double[ options.shiftsSmoothingRange + 1 ];
			gaussKernel[0] = 1.0;
			double normalizingConstant = gaussKernel[0];
			for ( int i = 1; i < gaussKernel.length; ++i ) {
				gaussKernel[ i ] = Math.exp( -0.5 * i * i / ( options.shiftsSmoothingSigma * options.shiftsSmoothingSigma ) );
				normalizingConstant += 2* gaussKernel[ i ];
			}
			
			for (int i = 0; i < gaussKernel.length; i++) {
				gaussKernel[ i ] /= normalizingConstant;
			}
			
			final OutOfBounds<DoubleType> mediatedRA = Views.extendMirrorSingle( mediatedShifts ).randomAccess();
			for (int i = 0; i < smoothedShifts.length; i++) {
				smoothedShifts[ i ] = 0.0;
				for ( int k = -options.shiftsSmoothingRange; k <= options.shiftsSmoothingRange; ++k ) {
					mediatedRA.setPosition( i + k, 0 );
					final double w = gaussKernel[ Math.abs( k ) ];
					final double val = mediatedRA.get().get() * w;
					smoothedShifts[ i ] += val;
				}
			}
			
			
			for ( int ijk = 0; mediatedCursor.hasNext(); ++ijk ) {
			
			    mediatedCursor.fwd();
			    final double previous = lut[ijk];
//			    lut[ijk] += accumulatedShifts + options.shiftProportion * mediatedCursor.get().get();
			    lut[ijk] += accumulatedShifts + options.shiftProportion * smoothedShifts[ ijk ];
			    if ( false && ijk < 0 )
			    	lut[ijk]  = Math.max( lut[ijk-1] + options.minimumSectionThickness, lut[ijk] );
			    // make sure, that slices do not flip positions, ie set lut[ijk] to
			    // previous + shiftProportion * ( lut[ijk-1] - previous )
			    // TODO think of a way of ensuring this (e.g. minimum section thickness?)
//			    if ( ijk > 0 && lut[ ijk ] < lut[ ijk - 1 ] ) {
//                	lut[ ijk ] = lut[ ijk - 1 ] + 0.1;
//                }
			    accumulatedShifts = 0.0; // lut[ijk] - previous; // TODO Why do accumulated shifts not work?
			}
			
			// TODO decide for complete range or end points for this fit
//			final float[] floatLut = new float[ lut.length ]; final float[] arange = new float[ lut.length ];
//			final float[] floatWeights = new float[ lut.length ];
//			for (int i = 0; i < arange.length; i++) {
//				arange[i] = i;
//				floatLut[i] = (float) lut[i];
//				floatWeights[i] = 1.0f;
//			}
			
			final float[] floatLut = new float[ 2 ]; final float[] arange = new float[ 2 ];
			final float[] floatWeights = new float[ 2 ];
			for (int i = 0; i < arange.length; i++) {
				arange[i] = i;
				floatWeights[i] = 1.0f;
			}
			floatLut[ 0 ] = (float) lut[ 0 ];
			arange[ 0 ]   = 0f;
			floatWeights[ 0 ] = 1.0f;
			floatLut[ 1 ] = (float) lut[ lut.length - 1 ];
			arange[ 1 ] = lut.length - 1;
			floatWeights[ 1 ] = 1.0f;
			
			final AffineModel1D coordinatesFitModel = new AffineModel1D();
			
			
			coordinatesFitModel.fit( new float[][]{floatLut}, new float[][]{arange}, floatWeights );
			
			final double[] affineArray = new double[ 2 ];
			coordinatesFitModel.toArray( affineArray );
			
			for (int i = 0; i < lut.length; i++) {
				lut[i] = affineArray[0] * lut[i] + affineArray[1];
			}
			
			visitor.act( n + 1, matrix, lut, transform, multipliers, weights, localFits.firstElement() );
        }
        
        
        


        public ArrayImg< DoubleType, DoubleArray > correlationsToMatrix( final long x, final long y ) {

                final int nSlices = this.correlationsObject.getMetaMap().size();
                final ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( nSlices, nSlices );
                for ( final DoubleType m : matrix ) {
                        m.set( Double.NaN );
                }

                for ( long zRef = this.zMin; zRef < this.zMax; ++zRef ) {
                        final long relativeZ = zRef - this.zMin;
                        final RandomAccessibleInterval<DoubleType> correlations = this.correlationsObject.extractDoubleCorrelationsAt( x, y, zRef ).getA();
                        final IntervalView<DoubleType> row = Views.hyperSlice( matrix, 1, relativeZ);

                        final RandomAccess<DoubleType> correlationsAccess = correlations.randomAccess();
                        final RandomAccess<DoubleType> rowAccess          = row.randomAccess();

                        final Meta meta = this.correlationsObject.getMetaMap().get( zRef );

                        rowAccess.setPosition( Math.max( meta.zCoordinateMin - this.zMin, 0 ), 0 );

                        for ( long zComp = meta.zCoordinateMin; zComp < meta.zCoordinateMax; ++zComp ) {
                                if ( zComp < this.zMin || zComp >= this.zMax ) {
                                        correlationsAccess.fwd( 0 );
                                        continue;
                                }
                                rowAccess.get().set( correlationsAccess.get() );
                                rowAccess.fwd( 0 );
                                correlationsAccess.fwd( 0 );

                        }

                }


                return matrix;
        }

        public static ArrayImg<FloatType, FloatArray> convertToFloat( final ArrayImg<DoubleType, DoubleArray> input ) {
                final long[] dims = new long[ input.numDimensions() ];
                input.dimensions( dims );
                final ArrayImg<FloatType, FloatArray> output = ArrayImgs.floats( dims );
                final ArrayCursor<DoubleType> i = input.cursor();
                final ArrayCursor<FloatType> o  = output.cursor();

                while( i.hasNext() ) {
                        o.next().set( i.next().getRealFloat() );
                }
                return output;
        }



        public static SingleDimensionLUTRealTransformField convertToTransformField2D( final ArrayImg< DoubleType, DoubleArray > input,
                                                                                       final int stepX,
                                                                                       final int stepY,
                                                                                       final long width,
                                                                                       final long height) {

                final long d = input.dimension( 2 );
                final double inverseStepX = 1.0/stepX;
                final double inverseStepY = 1.0/stepY;

                final ArrayImg<DoubleType, DoubleArray> tfs = ArrayImgs.doubles( new long[] { width, height, d } );
                final ArrayCursor<DoubleType> cursor = tfs.cursor();
                final RealRandomAccessible<DoubleType> interpolated = Views.interpolate( Views.extendBorder( input), new NLinearInterpolatorFactory<DoubleType>() );
                final RealRandomAccess<DoubleType> iAccess = interpolated.realRandomAccess();
                while ( cursor.hasNext() ) {
                	cursor.fwd();
                	iAccess.setPosition( cursor.getDoublePosition(0) * inverseStepX, 0 );
                	iAccess.setPosition( cursor.getDoublePosition(1) * inverseStepY, 1 );
                	iAccess.setPosition( cursor.getDoublePosition(2), 2 );
                	cursor.get().set( iAccess.get() );
                }

                return new SingleDimensionLUTRealTransformField( 3, 3, tfs );
        }


        public static void main(final String[] args) throws FunctionEvaluationException, NotEnoughDataPointsException, IllDefinedDataPointsException {



                //                final Random rng = new Random( 100 );
                //
                //                final boolean doPrint = false;
                //
                //                final int nData = 200;
                //                final int zMin  = 1;
                //                final int zMax  = zMin + nData;
                //                final double xScale   = 0.5;
                //                final double sigma    = 4.0;
                //                final int range = 25;
                //                final double gradient = -1.0 / range;
                //                final int nRep = 40;
                //
                //                final ArrayList<Double> coordinateBase  = new ArrayList<Double>();
                //                final ArrayList<Double> coordinateShift = new ArrayList<Double>();
                //                final ArrayList<Double> zShifts         = new ArrayList<Double>();
                //
                //
                //                for ( int n = 0; n < nData; ++n ) {
                //                        coordinateBase.add( (double) (n + 1) );
                //                        zShifts.add( 0.0 );
                //                        coordinateShift.add( (double) (n + 1) );
                //                }
                //
                //
                //                double prev = 0.0;
                //                for ( int n = range; n < nData - range; ++n ) {
                //                        coordinateBase.set(n, (double) (n + 1) );
                //                        zShifts.set( n, Math.abs( rng.nextGaussian() ) * xScale );
                //                        coordinateShift.set( n, Math.max( prev, coordinateBase.get(n) ) + zShifts.get(n) );
                //                        prev = coordinateShift.get( n );
                //                }
                //
                //
                //                if ( doPrint  ) {
                //                        System.out.println( coordinateBase );
                //                        System.out.println( zShifts );
                //                        System.out.println( coordinateShift );
                //                }
                //
                //                final double[] initialCoordinates = new double[ nData - 2*range ];
                //
                //                final TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > > corrs =
                //                                new TreeMap< ConstantTriple<Long, Long, Long>, ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> > >();
                //
                //                final TreeMap< Long, Meta > metaMap = new TreeMap<Long, Meta>();
                //
                //                for ( int i = range; i < nData - range; ++i ) {
                //
                ////                        System.out.println( i );
                //                        final ArrayImg<DoubleType, DoubleArray> measure = ArrayImgs.doubles( 2 * range + 1 );
                //                        final ArrayCursor<DoubleType> m = measure.cursor();
                //                        for ( int r = - range; r <= range; ++r ) {
                //                                m.next().set( new BellCurve().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), sigma } ) );
                ////                                m.next().set( new AbsoluteLinear().value( coordinateShift.get( i + r ), new double[] { coordinateShift.get( i ), 1.0, gradient } ) );
                ////                                m.next().set( Math.abs(coordinateShift.get( i + r ) -  coordinateShift.get( i ) ) * gradient + 1.0 );
                ////                                System.out.println( i + r + " " + m.get().get() );
                //                        }
                //
                //
                //                        final ArrayImg<DoubleType, DoubleArray> coord = ArrayImgs.doubles( 2 * range + 1 );
                //                        final ArrayCursor<DoubleType> c = coord.cursor();
                //                        for ( int r = - range; r <= range; ++r ) {
                //                                c.next().set( coordinateBase.get( i + r ) + zShifts.get( i + r ) );
                //                        }
                //
                //
                //
                //                        corrs.put( new ConstantTriple<Long, Long, Long>( 0l, 0l, (long) (i) ),
                //                                        new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( measure, coord ));
                //
                //                        final Meta meta = new Meta();
                //                        meta.zPosition = i;
                //                        meta.zCoordinateMin = i - range;
                //                        meta.zCoordinateMax = i + range + 1;
                //                        metaMap.put( (long) i, meta );
                //
                //                }
                //
                //
                ////                EstimateCorrelationsAtSamplePoints.arryImg = ArrayImgs.doubles( 11, 120, nRep );
                //
                //
                //
                //                final CorrelationsObjectInterface dummyCorrelationsObject = new DummyCorrelationsObject( zMin + range, zMax - range, range, nData, corrs, metaMap );
                //
                //                final InferFromCorrelationsObject<TranslationModel1D, ScaleModel> inf = new InferFromCorrelationsObject<TranslationModel1D, ScaleModel>(dummyCorrelationsObject,
                //                                nRep,
                //                                range,
                //                                new TranslationModel1D(),
                //                                new NLinearInterpolatorFactory<DoubleType>(),
                //                                new ScaleModel(),
                //                                1,
                //                                new OpinionMediatorModel<TranslationModel1D>( new TranslationModel1D() ) );
                //
                //                final ArrayImg<DoubleType, DoubleArray> matrix = inf.correlationsToMatrix( 0l, 0l );
                //
                //                // print matrix
                ////                for ( int i = 0; i < matrix.dimension( 0 ); ++i ) {
                ////                        for ( final DoubleType h : Views.iterable(Views.hyperSlice(matrix, 0, i) ) ) {
                ////                                System.out.print( h.get()+ ",");
                ////                        }
                ////                        System.out.println();
                ////                }
                //
                //
                //
                //                // System.exit(1);
                //
                //
                //
                //                final double[] noShiftCoordinates = new double[ initialCoordinates.length ];
                //                for (int i = 0; i < noShiftCoordinates.length; i++) {
                //                        noShiftCoordinates[i] = i;
                //                }
                //
                //                final ArrayImg<DoubleType, DoubleArray> coord = inf.estimateZCoordinates( 0, 0, noShiftCoordinates );
                //
                //                new ImageJ();
                ////                ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.arryImg );
                ////                ImageJFunctions.show( EstimateCorrelationsAtSamplePoints.matrixImg );
                //
                ////                for ( DoubleType c : coord ) {
                ////                        System.out.println( c.get() );
                ////                }
                //
                //
        }



}
