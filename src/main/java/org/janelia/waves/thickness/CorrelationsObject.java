package org.janelia.waves.thickness;

import java.util.HashMap;

import net.imglib2.Pair;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;




/**
 * A {@link CorrelatonsObject} calculates and stores the parameters of a fit to
 * correllation data. Use {@link CorrelationsObject.Options} to specify the fitter
 * as well as the sample ranges (by stride and fitIntervalLength) of the fit.
 * {@link CorrelationsObject.Meta} stores meta information, such as the position
 * of the current image in z-direction.
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 * 
 */
public class CorrelationsObject {
	public static class Meta {
		public long zPosition;
		public long zCoordinateMin;
		public long zCoordinateMax;
		
		@Override
		public String toString() {
			return new String("zPosition=" + this.zPosition +
					",zCoordinateMin=" + this.zCoordinateMin +
					",zCoordinateMax=" + this.zCoordinateMax);
		}
	}


	public static class Options {
		
	}

	private final HashMap<Long, RandomAccessibleInterval<FloatType> > correlationsMap;
	private final HashMap<Long, Meta> metaMap;
	private final HashMap<Long, RandomAccessibleInterval<FloatType> > fitMap;
	private Options options;
	
	
	
	
	
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


	
	public CorrelationsObject(
			final HashMap<Long, RandomAccessibleInterval<FloatType>> correlationsMap,
			final HashMap<Long, Meta> metaMap,
			final Options options) {
		super();
		this.correlationsMap = correlationsMap;
		this.metaMap = metaMap;
		this.fitMap = new HashMap<Long, RandomAccessibleInterval<FloatType>>();
		this.options = options;
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
	}
	
	/**
	 * Extract correlations and coordinates at (x,y,z)
	 * 
	 * @param x extract correlations at x
	 * @param y extract correlations at y
	 * @param z extract correlations at z
	 * @return {@link Pair} holding correlations and coordinates in terms of z slices. The actual "thicknesses" or real world coordinates need to be saved seperately. 
	 */
	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<LongType> > extractCorrelationsAt(long x, long y, long z) {
		IntervalView<FloatType> entryA      = Views.hyperSlice( Views.hyperSlice( correlationsMap.get( z ), 0, x ), 0, y );
		ArrayImg<LongType, LongArray> entryB = ArrayImgs.longs(entryA.dimension(0));
		
		long zPosition               = metaMap.get(z).zCoordinateMin;
		ArrayCursor<LongType> cursor = entryB.cursor();
		
		while ( cursor.hasNext() ) {
			cursor.next().set( zPosition );
			++zPosition;
		}
		
		assert zPosition == metaMap.get(z).zCoordinateMax: "Inconsistency!";
		
		return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<LongType> >( entryA, entryB );
	}
	
}
