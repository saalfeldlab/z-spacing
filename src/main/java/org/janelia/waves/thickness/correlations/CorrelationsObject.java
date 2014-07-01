package org.janelia.waves.thickness.correlations;

import java.util.HashMap;

import net.imglib2.Cursor;
import net.imglib2.Pair;
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
 * A {@link CorrelatonsObject} calculates and stores the parameters of a fit to
 * correllation data. Use {@link CorrelationsObject.Options} to specify the fitter
 * as well as the sample ranges (by stride and fitIntervalLength) of the fit.
 * {@link CorrelationsObject.Meta} stores meta information, such as the position
 * of the current image in z-direction.
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 */
public class CorrelationsObject implements CorrelationsObjectInterface {
	


	public static class Options {
		
	}

	private final HashMap<Long, RandomAccessibleInterval<FloatType> > correlationsMap;
	private final HashMap<Long, Meta> metaMap;
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
	public void setOptions(Options options) {
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
	public HashMap<Long, Meta> getMetaMap() {
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
	public long getzMin() {
		return zMin;
	}


	/**
	 * @return the zMax
	 */
	public long getzMax() {
		return zMax;
	}


	public CorrelationsObject(
			final HashMap<Long, RandomAccessibleInterval<FloatType>> correlationsMap,
			final HashMap<Long, Meta> metaMap,
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
			
			for ( Meta v : this.metaMap.values() ) {
				if ( v.zCoordinateMin < this.zMin ) {
					this.zMin = v.zCoordinateMin;
				}
				if ( v.zCoordinateMax > this.zMax ) {
					this.zMax = v.zCoordinateMax;
				}
			}
		}
	}


	public CorrelationsObject(Options options) {
		this(new HashMap<Long, RandomAccessibleInterval<FloatType>>(),
				new HashMap<Long, Meta>(),
				options);
	}
	
	
	public void addCorrelationImage(long index, 
			RandomAccessibleInterval<FloatType> correlations,
			Meta meta) 
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
	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> > extractCorrelationsAt(long x, long y, long z) {
		IntervalView<FloatType> entryA         = Views.hyperSlice( Views.hyperSlice( correlationsMap.get( z ), 0, x ), 0, y );
		ArrayImg<FloatType, FloatArray> entryB = ArrayImgs.floats(entryA.dimension(0));
		
		long zPosition               = metaMap.get(z).zCoordinateMin;
		ArrayCursor<FloatType> cursor = entryB.cursor();
		
		while ( cursor.hasNext() ) {
			cursor.next().set( zPosition );
			++zPosition;
		}
		
		assert zPosition == metaMap.get(z).zCoordinateMax: "Inconsistency!";
		
		return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType> >( entryA, entryB );
	}


	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
			long x, long y, long z) {
		IntervalView<FloatType> entryAFloat      = Views.hyperSlice( Views.hyperSlice( correlationsMap.get( z ), 0, x ), 0, y );
		ArrayImg<DoubleType, DoubleArray> entryA = ArrayImgs.doubles(entryAFloat.dimension(0));
		ArrayImg<DoubleType, DoubleArray> entryB = ArrayImgs.doubles(entryAFloat.dimension(0));
		
		long zPosition               = metaMap.get(z).zCoordinateMin;
		
		Cursor<FloatType> cursorFloat   = Views.flatIterable( entryAFloat ).cursor();
		ArrayCursor<DoubleType> cursorA = entryA.cursor();
		ArrayCursor<DoubleType> cursorB = entryB.cursor();
		
		while ( cursorA.hasNext() ) {
			cursorA.next().set( cursorFloat.next().getRealDouble() );
			cursorB.next().set( zPosition );
			++zPosition;
		}
		
		assert zPosition == metaMap.get(z).zCoordinateMax: "Inconsistency!";
		
		return new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType> >( entryA, entryB );
	}
	
}
 