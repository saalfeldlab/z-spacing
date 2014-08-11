package org.janelia.correlations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.Pair;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.utility.ConstantPair;




/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 * A {@link CorrelatonsObject} calculates and stores the parameters of a fit to
 * correllation data. Use {@link CorrelationsObject.Options} to specify the fitter
 * as well as the sample ranges (by stride and fitIntervalLength) of the fit.
 * {@link CorrelationsObject.Meta} stores meta information, such as the position
 * of the current image in z-direction.
 * 
 */
public class CorrelationsObject implements CorrelationsObjectInterface {
	


	public static class Options {
		
	}

	private final HashMap<Long, RandomAccessibleInterval<FloatType> > correlationsMap;
	private final TreeMap<Long, Meta> metaMap;
	private final HashMap<Long, RandomAccessibleInterval<FloatType> > fitMap;
	private Options options;
	private long zMin;
	private long zMax;
	
	
	
	
	
	/**
	 * @return the options
	 */
	public Options getOptions() {
		return options;
	}


	/**
	 * @param options the options to set
	 */
	public void setOptions(final Options options) {
		this.options = options;
	}


	/**
	 * @return the correlationsMap
	 */
	public HashMap<Long, RandomAccessibleInterval<FloatType>> getCorrelationsMap() {
		return correlationsMap;
	}


	/**
	 * @return the metaMap
	 */
	@Override
	public TreeMap<Long, Meta> getMetaMap() {
		return metaMap;
	}


	/**
	 * @return the fitMap
	 */
	public HashMap<Long, RandomAccessibleInterval<FloatType>> getFitMap() {
		return fitMap;
	}

	
	/**
	 * @return the zMin
	 */
	@Override
	public long getzMin() {
		return zMin;
	}


	/**
	 * @return the zMax
	 */
	@Override
	public long getzMax() {
		return zMax;
	}


	public CorrelationsObject(
			final HashMap<Long, RandomAccessibleInterval<FloatType>> correlationsMap,
			final TreeMap<Long, Meta> metaMap,
			final Options options) {
		super();
		this.zMin = 0;
		this.zMax = 0;
		this.correlationsMap = correlationsMap;
		this.metaMap = metaMap;
		this.fitMap = new HashMap<Long, RandomAccessibleInterval<FloatType>>();
		this.options = options;
		
		if ( this.metaMap.size() != 0 ) {
			this.zMin = this.metaMap.values().iterator().next().zCoordinateMin;
			this.zMax = this.metaMap.values().iterator().next().zCoordinateMax;
			
			for ( final Meta v : this.metaMap.values() ) {
				if ( v.zCoordinateMin < this.zMin ) {
					this.zMin = v.zCoordinateMin;
				}
				if ( v.zCoordinateMax > this.zMax ) {
					this.zMax = v.zCoordinateMax;
				}
			}
		}
	}


	public CorrelationsObject(final Options options) {
		this(new HashMap<Long, RandomAccessibleInterval<FloatType>>(),
				new TreeMap<Long, Meta>(),
				options);
	}
	
	
	public void addCorrelationImage(final long index, 
			final RandomAccessibleInterval<FloatType> correlations,
			final Meta meta) 
	{
		this.correlationsMap.put(index, correlations);
		this.metaMap.put(index, meta);
		if ( meta.zCoordinateMin < this.zMin ) {
			this.zMin = meta.zCoordinateMin;
		}
		
		if ( meta.zCoordinateMax > this.zMax ) {
			this.zMax = meta.zCoordinateMax;
		}
	}
	
	/**
	 * Extract correlations and coordinates at (x,y,z)
	 * 
	 * @param x extract correlations at x
	 * @param y extract correlations at y
	 * @param z extract correlations at z
	 * @return {@link Pair} holding correlations and coordinates in terms of z slices. The actual "thicknesses" or real world coordinates need to be saved seperately. 
	 */
	@Override
	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> > extractCorrelationsAt(final long x, final long y, final long z) {
		final IntervalView<FloatType> entryA         = Views.hyperSlice( Views.hyperSlice( correlationsMap.get( z ), 0, x ), 0, y );
		final ArrayImg<FloatType, FloatArray> entryB = ArrayImgs.floats(entryA.dimension(0));
		
		long zPosition               = metaMap.get(z).zCoordinateMin;
		final ArrayCursor<FloatType> cursor = entryB.cursor();
		
		while ( cursor.hasNext() ) {
			cursor.next().set( zPosition );
			++zPosition;
		}
		
		assert zPosition == metaMap.get(z).zCoordinateMax: "Inconsistency!";
		
		return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> >( entryA, entryB );
	}


	@Override
	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
			final long x, final long y, final long z) {
		final IntervalView<FloatType> entryAFloat      = Views.hyperSlice( Views.hyperSlice( correlationsMap.get( z ), 0, x ), 0, y );
		final ArrayImg<DoubleType, DoubleArray> entryA = ArrayImgs.doubles(entryAFloat.dimension(0));
		final ArrayImg<DoubleType, DoubleArray> entryB = ArrayImgs.doubles(entryAFloat.dimension(0));
		
		long zPosition               = metaMap.get(z).zCoordinateMin;
		
		final Cursor<FloatType> cursorFloat   = Views.flatIterable( entryAFloat ).cursor();
		final ArrayCursor<DoubleType> cursorA = entryA.cursor();
		final ArrayCursor<DoubleType> cursorB = entryB.cursor();
		
		while ( cursorA.hasNext() ) {
			cursorA.next().set( cursorFloat.next().getRealDouble() );
			cursorB.next().set( zPosition );
			++zPosition;
		}
		
		assert zPosition == metaMap.get(z).zCoordinateMax: "Inconsistency!";
		
		return new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( entryA, entryB );
	}


	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix( 
			final long x, 
			final long y) {
		final Iterator<Long> iterator = this.metaMap.keySet().iterator();
        final long zMin = iterator.next();
        long zMaxTmp = zMin;

        while ( iterator.hasNext() )
                zMaxTmp = iterator.next();
        final long zMax = zMaxTmp + 1;
        return toMatrix( x, y, zMin, zMax );
	}
	
	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix( 
			final long x, 
			final long y, 
			final long zMin, 
			final long zMax ) {
		 final int nSlices = this.getMetaMap().size();
         final ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( nSlices, nSlices );

         this.toMatrix(zMax, y, matrix);

         return matrix;
	}


	@Override
	public void toMatrix(final long x, final long y,
			final RandomAccessibleInterval<DoubleType> matrix) {
		for ( final DoubleType m : Views.flatIterable( matrix ) ) {
            m.set( Double.NaN );
    }
    

    for ( long zRef = zMin; zRef < zMax; ++zRef ) {
   	     final RandomAccessibleInterval<FloatType> correlationsAt = this.correlationsMap.get( zRef );
   	     System.out.println( x + " " + y + " " + zRef + " " + correlationsAt);
   	 
            final long relativeZ = zRef - zMin;
            final IntervalView<DoubleType> row = Views.hyperSlice( matrix, 1, relativeZ);

            final RandomAccess<FloatType> correlationsAccess = correlationsAt.randomAccess();
            final RandomAccess<DoubleType> rowAccess         = row.randomAccess();
            
            correlationsAccess.setPosition( x, 0 );
            correlationsAccess.setPosition( y, 1 );
            correlationsAccess.setPosition( 0, 2 );

            final Meta meta = this.metaMap.get( zRef );

            rowAccess.setPosition( Math.max( meta.zCoordinateMin - zMin, 0 ), 0 );

            for ( long zComp = meta.zCoordinateMin; zComp < meta.zCoordinateMax; ++zComp ) {
                    if ( zComp < zMin || zComp >= zMax ) {
                            correlationsAccess.fwd( 2 );
                            continue;
                    }
                    rowAccess.get().set( correlationsAccess.get().getRealDouble() );
                    rowAccess.fwd( 0 );
                    correlationsAccess.fwd( 2 );

            }

    }

	}
	
	
}
 