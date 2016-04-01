package org.janelia.thickness.inference.fits;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import net.imglib2.*;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.list.ListImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.lut.AbstractLUTRealTransform;
import org.janelia.thickness.lut.LUTRealTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hanslovskyp on 10/7/15.
 */
public abstract class AbstractCorrelationFit {

    protected static final double[] ONE_DIMENSION_ZERO_POSITION = new double[]{ 0.0 };

    public < T extends RealType< T >> RealRandomAccessible<RealComposite<DoubleType>> estimateFromMatrix(
            final RandomAccessibleInterval< T > correlations,
            final double[] coordinates,
            final double[] weights,
            final double[] multipliers,
            final AbstractLUTRealTransform transform,
            Options options )
    {
        int range = options.comparisonRange;
        boolean forceMonotonicity = options.forceMonotonicity;
        int estimateWindowRadius = options.estimateWindowRadius > 0 ? options.estimateWindowRadius : coordinates.length - 1;
        int nFits = (int) Math.ceil( (coordinates.length - estimateWindowRadius) * 1.0 / estimateWindowRadius );

        final T correlationsNaNExtension = correlations.randomAccess().get().copy();
        correlationsNaNExtension.setReal(Double.NaN);
        final RealRandomAccessible< T > extendedInterpolatedCorrelations =
                Views.interpolate(Views.extendValue(correlations, correlationsNaNExtension), new NLinearInterpolatorFactory<T>());

        final RealTransformRealRandomAccessible< T, InverseRealTransform> transformedCorrelations =
                RealViews.transformReal(extendedInterpolatedCorrelations, transform);
        final LUTRealTransform transform1d = new LUTRealTransform( coordinates, 1, 1);
        final RealTransformRealRandomAccessible<DoubleType, InverseRealTransform> multipliersInterpolatedTransformed =
                RealViews.transformReal(
                        Views.interpolate(
                                Views.extendValue( ArrayImgs.doubles(multipliers, multipliers.length),
                                new DoubleType( Double.NaN ) ),
                                new NLinearInterpolatorFactory<DoubleType>()),
                        transform1d );

        final RealRandomAccess< T > access1  = transformedCorrelations.realRandomAccess();
        final RealRandomAccess< T > access2 = transformedCorrelations.realRandomAccess();

        ArrayImg<DoubleType, DoubleArray> fits = ArrayImgs.doubles(nFits, range + 1);
        CompositeIntervalView<DoubleType, RealComposite<DoubleType>> result = Views.collapseReal(fits);

        for( int z = estimateWindowRadius, zIndex = 0; z < coordinates.length; z += estimateWindowRadius, ++zIndex )
        {
            int min = z - estimateWindowRadius;
            int max = Math.min(z + estimateWindowRadius + 1, coordinates.length);

            ArrayList<ArrayList<PointMatch>> samples = new ArrayList<ArrayList<PointMatch>>();

            for ( int r = 0; r <= range; ++r )
                samples.add( new ArrayList<PointMatch>() );

            for ( int i = min; i < max; ++i ) {

                access1.setPosition(i, 1);
                access1.setPosition(i, 0);

                transform.apply(access1, access1);
                access2.setPosition(access1);

                double currentMin1 = Double.MAX_VALUE;
                double currentMin2 = Double.MAX_VALUE;

                for (int k = 0; k <= range; ++k, access1.fwd(0), access2.bck(0) ) {

                    final double a1 = access1.get().getRealDouble();
                    final double a2 = access2.get().getRealDouble();

                    if ((!Double.isNaN(a1)) && (a1 > 0.0) && (!forceMonotonicity || (a1 < currentMin1))) {
                        currentMin1 = a1;
                        final double w1 = 1.0;
                        samples.get(k).add(new PointMatch(new Point(ONE_DIMENSION_ZERO_POSITION), new Point(new double[]{ a1 }), w1));
                    }

                    if ((!Double.isNaN(a2)) && (a2 > 0.0) && (!forceMonotonicity || (a2 < currentMin2))) {
                        currentMin2 = a2;
                        final double w2 = 1.0;
                        samples.get(k).add(new PointMatch(new Point(ONE_DIMENSION_ZERO_POSITION), new Point(new double[]{ a2 }), w2));
                    }
                }
            }

            {
                Cursor<DoubleType> fitCursor = Views.flatIterable(Views.hyperSlice(fits, 0, zIndex)).cursor();
                fitCursor.next().set(-1.0); // do not measure for delta z == 0
                for (int index = 1; fitCursor.hasNext(); ++index)
                    fitCursor.next().set(-estimate(samples.get(index)));
            }

            {
                Cursor<DoubleType> fitCursor1 = Views.hyperSlice(fits, 0, zIndex).cursor();
                Cursor<DoubleType> fitCursor2 = Views.hyperSlice(fits, 0, zIndex).cursor();
                fitCursor1.fwd();
                fitCursor2.fwd();
                double val = 0.5 * (3.0 * fitCursor1.next().get() - fitCursor1.next().get() );
                double reciprocal = -1.0 / val;
                while ( fitCursor2.hasNext() )
                    fitCursor2.next().mul( reciprocal );
            }
        }

        CompositeIntervalView<DoubleType, RealComposite<DoubleType>> collapsed = Views.collapseReal(fits);

        ExtendedRandomAccessibleInterval<RealComposite<DoubleType>, CompositeIntervalView<DoubleType, RealComposite<DoubleType>>> extended =
                Views.extendBorder(collapsed);

        RealRandomAccessible<RealComposite<DoubleType>> interpolated =
                Views.interpolate(extended, new NLinearInterpolatorFactory<RealComposite<DoubleType>>());

        return RealViews.transform(
                interpolated,
                new ScaleAndTranslation( new double[] { estimateWindowRadius }, new double[] { estimateWindowRadius }  ) );
    }

    public void raster( RealRandomAccessible<RealComposite<DoubleType>> source, RandomAccessibleInterval<double[]> target )
    {
        RealRandomAccess<RealComposite<DoubleType>> ra = source.realRandomAccess();
        for(  Cursor<double[]> c = Views.flatIterable( target ).cursor(); c.hasNext(); )
        {
            double[] t = c.next();
            ra.setPosition( c );
            RealComposite<DoubleType> s = ra.get();
            for( int z = 0; z < t.length; ++z ) {
                t[z] = s.get(z).get();
            }
        }
    }


    public void raster(RealRandomAccessible<RealComposite<DoubleType>> source, List<double[]> target)
    {
        raster( source, new ListImg<double[]>( target, target.size() ) );
    }


    protected abstract double estimate( ArrayList<PointMatch> measurements );

}
