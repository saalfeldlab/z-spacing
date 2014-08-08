package org.janelia.thickness;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math.FunctionEvaluationException;
import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.TransformRandomAccessibleInterval;
import org.janelia.thickness.mediator.OpinionMediator;
import org.janelia.utility.ConstantPair;

public class InferFromCorrelationsObject< M extends Model<M>, L extends Model<L> > {

        private final CorrelationsObjectInterface correlationsObject;
        private final int nIterations;
        private final int comparisonRange;
        private final M correlationFitModel;
        private final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory;
        private final L measurementsMultiplierModel;
        private final int nThreads;
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
                        return result;
                }

                public double multiplierGenerationRegularizerWeight; // m_regularized = m * ( 1 - w ) + 1 * w
                public double coordinateUpdateRegularizerWeight; // coordinate_regularized = predicted * ( 1 - w ) + original * w
                public double shiftProportion; // actual_shift = shift * shiftProportion
                public int nIterations; // number of iterations
                public int nThreads; // number of threads
                public int comparisonRange; // range for cross correlations
                public double neighborRegularizerWeight;


        }


        public InferFromCorrelationsObject(
                final CorrelationsObjectInterface correlationsObject,
                final int nIterations,
                final int comparisonRange,
                final M correlationFitModel,
                final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType>> fitInterpolatorFactory,
                final L measurementsMultiplierModel,
                final int nThreads,
                final OpinionMediator shiftMediator ) {
                super();

                this.correlationsObject = correlationsObject;
                this.nIterations = nIterations;
                this.comparisonRange = comparisonRange;
                this.correlationFitModel = correlationFitModel;
                this.fitInterpolatorFactory = fitInterpolatorFactory;
                this.measurementsMultiplierModel = measurementsMultiplierModel;
                this.nThreads = nThreads;
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

                for ( int n = 0; n < this.nIterations; ++n ) {

                        this.iterationStep(matrix, weightArr, transform, lut, coordinateArr, coordinates, mediatedShifts, options, visitor, n);

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

                final double ownWeight = 1.0 - options.neighborRegularizerWeight;

                final double[] defaultWeights = new double[ defaultCoordinates.length ];
                for (int i = 0; i < defaultWeights.length; i++) {
                        defaultWeights[i] = 1.0;
                }


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
                                if ( matrix == null ) {
                                        System.out.println( "matrix == 0: " + x + "," + y );
                                }
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
                                        double weightSum = ownWeight;
                                        for ( int z = 0; z < c.length; ++z ) {
                                                c[ z ] *= ownWeight;
                                        }
                                        for ( int dy = -1; dy < 2; ++dy ) {
                                                for ( int dx = -1; dx < 2; ++dx ) {
                                                        final int xPos = nx + dx;
                                                        final int yPos = ny + dy;
                                                        if ( ( dy == 0 && dx == 0 ) || xPos < 0 || yPos < 0 || xPos >= nX || yPos >= nY ) {
                                                                continue;
                                                        }
                                                        final double weight = options.neighborRegularizerWeight;
                                                        final double[] cNeighbor = previousCoordinates.get( new ConstantPair< Integer, Integer>( xPos, yPos ) );
                                                        //                                                        System.out.println( xPos + " " + yPos + " " + ( cNeighbor == null ) + " " + ( coordinatesMap.get( new ConstantPair< Integer, Integer>( xPos, yPos ) ) == null ) );
                                                        for ( int z = 0; z < c.length; ++z ) {
                                                                c[ z ] += weight*cNeighbor[ z ];
                                                        }
                                                        weightSum += weight;
                                                }
                                        }
                                        for ( int z = 0; z < c.length; ++ z ) {
                                                c[z] /= weightSum;
                                        }
                                }
                        }
                }



                final ArrayImg<DoubleType, DoubleArray> result = ArrayImgs.doubles( nX, nY, defaultCoordinates.length );
                final ArrayCursor<DoubleType> resultCursor = result.cursor();
                while( resultCursor.hasNext() ) {
                        resultCursor.fwd();
                        resultCursor.get().set( coordinatesMap.get( new ConstantPair<Integer, Integer>( resultCursor.getIntPosition( 0 ), resultCursor.getIntPosition( 1 ) ) )[ resultCursor.getIntPosition( 2 ) ] - resultCursor.getIntPosition( 2 )  );
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
                final double[] vars = new double[ this.comparisonRange ];

                final double[] estimatedFit = EstimateCorrelationsAtSamplePoints.estimateFromMatrix( matrix, weights, transform, coordinateArr, this.comparisonRange, this.correlationFitModel, vars );

                final double inverseCoordinateUpdateRegularizerWeight = 1 - options.coordinateUpdateRegularizerWeight;

                final double[] multipliers = EstimateQualityOfSlice.estimateFromMatrix( matrix,
                                                                                        weights,
                                                                                        this.measurementsMultiplierModel,
                                                                                        coordinates,
                                                                                        mirrorAndExtend( estimatedFit, new NLinearInterpolatorFactory< DoubleType >() ),
                                                                                        this.nThreads,
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
                final ArrayCursor<DoubleType> coordinateCursor = coordinates.cursor();


                int ijk = 0;


                while ( mediatedCursor.hasNext() ) {

                        coordinateCursor.fwd();
                        mediatedCursor.fwd();

                        lut[ijk] += options.shiftProportion * mediatedCursor.get().get();
                        lut[ijk] *= inverseCoordinateUpdateRegularizerWeight;
                        lut[ijk] += options.coordinateUpdateRegularizerWeight * ijk;

                        ++ijk;

                }

                visitor.act( n + 1, matrix, lut, transform, multipliers, weights, estimatedFit );
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
