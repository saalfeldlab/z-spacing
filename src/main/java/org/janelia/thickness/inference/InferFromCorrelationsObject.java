package org.janelia.thickness.inference;

import ij.IJ;

import java.util.ArrayList;
import java.util.Iterator;
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
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.list.ListCursor;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.thickness.EstimateQualityOfSlice;
import org.janelia.thickness.LocalizedCorrelationFit;
import org.janelia.thickness.ShiftCoordinates;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.SingleDimensionLUTRealTransformField;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.ArraySortedIndices;
import org.janelia.utility.ConstantPair;

public class InferFromCorrelationsObject< M extends Model<M>, L extends Model<L> > {

        private final CorrelationsObjectInterface correlationsObject;
        private final M correlationFitModel;
        private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory;
        private final L measurementsMultiplierModel;
        private final OpinionMediator shiftMediator;
        private final long zMin;
        private final long zMax;


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
                                        final double[] estimatedFit,
                            			final int[] positions ) {
                                // don't do anything
                        }},
                        options );
        }


        public ArrayImg< DoubleType, DoubleArray > estimateZCoordinates(
                final long x,
                final long y,
                final double[] startingCoordinates,
                final Visitor visitor,
                final Options options) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

                final ArrayImg<DoubleType, DoubleArray> matrix = this.correlationsToMatrix( x, y );

                final double[] weightArr = new double[ ( int )matrix.dimension( 1 ) ];
                final ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( weightArr, new long[]{ weightArr.length } );

                for ( final DoubleType w : weights) {
                        w.set( 1.0 );
                }

                final double[] originalCoordinates = startingCoordinates.clone();
                final double[] lut = startingCoordinates;
                final double[] coordinateArr = lut;


                final ArrayImg<DoubleType, DoubleArray> coordinates = ArrayImgs.doubles( coordinateArr, coordinateArr.length );


                final LUTRealTransform transform = new LUTRealTransform(lut, matrix.numDimensions(), matrix.numDimensions() );

                final ArrayImg<DoubleType, DoubleArray> mediatedShifts = ArrayImgs.doubles( lut.length );



                visitor.act( 0, matrix, lut, transform, weightArr, weightArr, new double[ lut.length ], null );
                
                final double[] weightTable = new double[ lut.length ];
                for (int i = 0; i < weightTable.length; i++) {
					weightTable[ i ] = Math.exp( - 0.5*i*i / 40000 );
				}
                

				final LocalizedCorrelationFit lcf = new LocalizedCorrelationFit( );
				final ArrayList<double[]> fitList = new ArrayList< double[] >();
				for ( int i = 0; i < lut.length; ++i ) {
					fitList.add( new double[ options.comparisonRange ] );
				}
				
				final ListImg< double[] > localFits = new ListImg<double[]>( fitList, fitList.size() );
				
				final double[] multipliers = new double[ weightArr.length ];
				for ( int i = 0; i < multipliers.length; ++i )
					multipliers[i] = 1.0;
				
				final int[] orderedIndices = new int[ weightArr.length ];
				for (int i = 0; i < orderedIndices.length; ++i )
					orderedIndices[ i ] = i;

                for ( int n = 0; n < options.nIterations; ++n ) {
                        
                        this.iterationStep(matrix, weightArr, transform, lut, coordinateArr, coordinates, mediatedShifts, options, visitor, n, lcf, localFits, multipliers, orderedIndices, originalCoordinates );
                        IJ.showProgress( n, options.nIterations );
//                        IJ.log( Arrays.toString( orderedIndices ) );
                }
                return coordinates;
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
                final ListImg< double[] > localFits,
                final double[] multipliers,
                final int[] orderedIndices,
                final double[] originalCoordinates
                ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
			final double[] vars = new double[ options.comparisonRange ];
			
			if ( n < 0 ) {
				final double[] initialFit = localFits.firstElement().clone();
				for ( int i = 0; i < initialFit.length; ++i ) {
					initialFit[ i ] = 1 - i * 1.0 / initialFit.length;
				}
				final ListCursor<double[]> lf = localFits.cursor();
				while ( lf.hasNext() ) {
					lf.fwd();
					lf.set( initialFit );
				}
			} else 
				lcf.estimateFromMatrix(matrix, coordinateArr, weights, multipliers, transform, options.comparisonRange, correlationFitModel, localFits, options.windowRange);
//			}	
			
			final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;
			
			if ( n < 0 ) {
				for (int i = 0; i < multipliers.length; i++) {
					multipliers[i] = 1.0;
				}
			} else {
			EstimateQualityOfSlice.estimateQuadraticFromMatrix( matrix, 
					weights, 
					multipliers, 
					lut, 
					localFits, 
					options.multiplierGenerationRegularizerWeight, 
					options.comparisonRange, 
					options.multiplierEstimationIterations );
			}
			
			
			for ( int i = 0; i < multipliers.length; ++i ) {
				final double diff = 1.0 - multipliers[ i ];
				weights[ i ] = Math.exp( -0.5*diff*diff / ( options.multiplierWeightsSigma ) );
			}
			
			final TreeMap< Long, ArrayList< ConstantPair< Double, Double > > > shifts =
			            ShiftCoordinates.collectShiftsFromMatrix(
			                    coordinateArr,
			                    matrix,
			                    weights,
			                    multipliers,
			                    localFits );
			
			this.shiftMediator.mediate( shifts, mediatedShifts );
			
			final ArrayCursor<DoubleType> mediatedCursor = mediatedShifts.cursor();
			
			final double[] smoothedShifts = new double[ (int) mediatedShifts.dimension( 0 ) ];
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
			
			final OutOfBounds<DoubleType> mediatedRA    = Views.extendMirrorSingle( mediatedShifts ).randomAccess();
			final OutOfBounds<DoubleType> weightsRA     = Views.extendMirrorSingle( ArrayImgs.doubles( multipliers, multipliers.length ) ).randomAccess();
			for (int i = 0; i < smoothedShifts.length; i++) {
				smoothedShifts[ i ] = 0.0;
				double weightSum = 0.0;
				for ( int k = -options.shiftsSmoothingRange; k <= options.shiftsSmoothingRange; ++k ) {
					mediatedRA.setPosition( i + k, 0 );
					weightsRA.setPosition( mediatedRA );
					final double w = gaussKernel[ Math.abs( k ) ] * weightsRA.get().get();
					final double val = mediatedRA.get().get() * w;
					smoothedShifts[ i ] += val;
					weightSum += w;
				}
				smoothedShifts[ i ] /= weightSum;
			}
			
			
			double accumulatedCorrections = 0.0;
			for ( int ijk = 0; mediatedCursor.hasNext(); ++ijk ) {
			
			    mediatedCursor.fwd();
			    lut[ijk] += accumulatedCorrections + options.shiftProportion * smoothedShifts[ ijk ];
			    lut[ijk] = options.coordinateUpdateRegularizerWeight * originalCoordinates[ijk] + inverseCoordinateUpdateRegularizerWeight * lut[ijk];
			    final double previous = lut[ijk];
			    if ( ijk > 0 && lut[ijk] < lut[ijk-1] + options.minimumSectionThickness ) {
			    	
			    	lut[ijk]  = lut[ijk-1] +  options.minimumSectionThickness;
			    	accumulatedCorrections += lut[ijk] - previous;
			    }

			}
			
			
			if ( options.withReorder ) {
				final TreeMap<Double, Integer> tm = ArraySortedIndices.sortedKeysAndValues( lut );
				final int[] indices   = ArraySortedIndices.getSortedIndicesFromMap( tm );
				final double[] lutTmp = ArraySortedIndices.getSortedArrayFromMap( tm );
				final int[] orderedIndicesTmp = orderedIndices.clone();
				for (int i = 0; i < lutTmp.length; i++) {
					lut[i] = lutTmp[i];
					// check in indices, where the value at i should go and write into the array at indices[i]
					orderedIndices[i] = orderedIndicesTmp[indices[i]];
				}
				
				// reorder matrix
				final ArrayImg<DoubleType, ? > tmpMatrix = matrix.copy(); // ArrayImgs.doubles( matrix.dimension( 0 ), matrix.dimension( 1 ) );
				final ArrayCursor<DoubleType> mC         = matrix.cursor();
				final ArrayRandomAccess<DoubleType> ra   = tmpMatrix.randomAccess();
				
				
				// run over dummy matrix (tmp) and check in indices, where the value at mC should go. set coordinates of ra accordingly and write into it
				while( mC.hasNext() ) {
					mC.fwd();
					ra.setPosition( indices[ mC.getIntPosition( 0 )], 0 );
					ra.setPosition( indices[ mC.getIntPosition( 1 )], 1 );
					mC.get().set( ra.get() );
				}
			}
			
			if ( options.withRegularization ) {
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
			}
			
			
			visitor.act( n + 1, matrix, lut, transform, multipliers, weights, localFits.firstElement(), orderedIndices );
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

}
