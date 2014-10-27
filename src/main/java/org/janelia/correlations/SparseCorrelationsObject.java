/**
 * 
 */
package org.janelia.correlations;

import ij.ImageJ;

import java.util.Set;
import java.util.TreeMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.utility.ConstantPair;
import org.janelia.utility.SerializableConstantPair;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SparseCorrelationsObject extends AbstractCorrelationsObject implements CorrelationsObjectInterface {
	
	private final TreeMap< SerializableConstantPair< Long, Long >, TreeMap< Long, double[] > > correlations;
	private long xMin;
	private long yMin;
	private long xMax;
	private long yMax;
	
	
	
	

	/**
	 * @param metaMap
	 * @param correlations
	 */
	public SparseCorrelationsObject(final TreeMap<Long, Meta> metaMap,
			final TreeMap<SerializableConstantPair<Long, Long>, TreeMap< Long, double[] > > correlations) {
		super(metaMap);
		this.correlations = correlations;
		xMin = Long.MAX_VALUE;
		yMin = Long.MAX_VALUE;
		xMax = Long.MIN_VALUE;
		yMax = Long.MIN_VALUE;
		
		for ( final SerializableConstantPair<Long, Long> key : correlations.keySet() ) {
			xMin = Math.min( xMin, key.getA() );
			yMin = Math.min( yMin, key.getB() );
			xMax = Math.max( xMax, key.getA() );
			yMax = Math.max( yMax, key.getB() );
		}
		
	}

	public SparseCorrelationsObject() {
		this( new TreeMap< Long, Meta > (), new TreeMap< SerializableConstantPair< Long, Long >, TreeMap< Long, double[] > >() );
//		super(new TreeMap< Long, Meta > () );
//		this.correlations = new TreeMap< SerializableConstantPair< Long, Long >, TreeMap< Long, double[] > >();
	}
	
	public void addCorrelationsAt( final long x, final long y, final long z, final double[] corrs, final Meta meta ) {
		this.addCorrelationsAt(x, y, z, corrs);
		this.addToMeta( z, meta );
	}
	
	public void addCorrelationsAt( final long x, final long y, final long z, final double[] corrs ) {
		TreeMap<Long, double[]> correlationsAt = this.correlations.get( ConstantPair.toPair( x, y ) );
		if ( correlationsAt == null ) {
			correlationsAt = new TreeMap< Long, double[] >();
			this.correlations.put( SerializableConstantPair.toPair( x, y ), correlationsAt );
		}
		correlationsAt.put( z, corrs );
		xMin = Math.min( xMin, x );
		yMin = Math.min( yMin, y );
		xMax = Math.max( xMax, x );
		yMax = Math.max( yMax, y );
	}

	/* (non-Javadoc)
	 * @see org.janelia.correlations.CorrelationsObjectInterface#extractCorrelationsAt(long, long, long)
	 */
	@Override
	public ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>> extractCorrelationsAt(
			final long x, final long y, final long z) {
		final double[] correlationsAt = getCorrelationsAt(x, y, z );
		if ( correlationsAt == null ) {
			return null;
		}
		final float[] correlationResult = new float[ correlationsAt.length ];
		final float[] coordinatesResult = new float[ correlationResult.length ];
		for (int i = 0; i < correlationResult.length; i++) {
			correlationResult[ i ] = (float) correlationsAt[ i ];
			coordinatesResult[ i ] = i;
		}
		return new ConstantPair<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>( ArrayImgs.floats( correlationResult, correlationResult.length), ArrayImgs.floats(coordinatesResult, coordinatesResult.length ) );
	}

	/* (non-Javadoc)
	 * @see org.janelia.correlations.CorrelationsObjectInterface#extractDoubleCorrelationsAt(long, long, long)
	 */
	@Override
	public ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>> extractDoubleCorrelationsAt(
			final long x, final long y, final long z) {
		final double[] correlationsAt = getCorrelationsAt(x, y, z );
		if ( correlationsAt == null ) {
			return null;
		}
		final double[] coordinatesResult = new double[ correlationsAt.length ];
		for (int i = 0; i < coordinatesResult.length; i++) {
			coordinatesResult[ i ] = i;
		}
		return new ConstantPair<RandomAccessibleInterval<DoubleType>, RandomAccessibleInterval<DoubleType>>( ArrayImgs.doubles( correlationsAt, correlationsAt.length ), ArrayImgs.doubles( coordinatesResult, coordinatesResult.length ) );
	}
	
	
	protected double[] getCorrelationsAt( final long x, final long y, final long z ) {
		final TreeMap< Long, double[] > correlationsAt = this.correlations.get( new ConstantPair< Long, Long >( x, y ));
		if ( correlationsAt == null )
			return null;
		return correlationsAt.get( z );
	}
	
	
	/* (non-Javadoc)
	 * @see org.janelia.correlations.CorrelationsObjectInterface#toMatrix(long, long)
	 */
	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y) {
		if(  this.correlations.get( new ConstantPair< Long, Long >( x, y )) == null )
			return null;
		final long nEntries = this.zMax - this.zMin;
		final ArrayImg<DoubleType, DoubleArray> matrix = ArrayImgs.doubles( nEntries, nEntries );
		this.toMatrix(x, y, matrix);
		return matrix;
	}

	/* (non-Javadoc)
	 * @see org.janelia.correlations.CorrelationsObjectInterface#toMatrix(long, long, net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public void toMatrix(final long x, final long y,
			final RandomAccessibleInterval<DoubleType> matrix) {
		final TreeMap<Long, double[]> correlationsAtXY = this.correlations.get( new ConstantPair< Long, Long >( x, y ));
		if ( correlationsAtXY == null )
			return;
		for ( final DoubleType m : Views.flatIterable( matrix ) )
			m.set( Double.NaN );
		
		for ( long zRef = zMin; zRef < zMax; ++zRef ) {
	   	    final double[] correlationsAt = correlationsAtXY.get( zRef );
            final long relativeZ = zRef - zMin;
            final IntervalView<DoubleType> row = Views.hyperSlice( matrix, 1, relativeZ);

            final RandomAccess<DoubleType> rowAccess         = row.randomAccess();
            
            final Meta meta = this.metaMap.get( zRef );

            rowAccess.setPosition( Math.max( meta.zCoordinateMin - zMin, 0 ), 0 );

            for ( long zComp = meta.zCoordinateMin; zComp < meta.zCoordinateMax; ++zComp ) {
                    rowAccess.get().set( correlationsAt[ (int) (zComp - meta.zCoordinateMin) ] );
                    rowAccess.fwd( 0 );
            }
	    }
	}

	/* (non-Javadoc)
	 * @see org.janelia.correlations.CorrelationsObjectInterface#toMatrix(long, long, long, long)
	 */
	@Override
	public ArrayImg<DoubleType, DoubleArray> toMatrix(final long x, final long y,
			final long zMin, final long zMax) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public static void main(final String[] args) {
		
		final SparseCorrelationsObject sco = new SparseCorrelationsObject();
		
		final int range = 12;
		for ( int i = 0; i < 10; ++i ) {
			final int iMin = Math.max( 0, i-range );
			final int iMax = Math.min( 10, i+range+1 );
			final double[] corrs = new double[ iMax - iMin ];
			for ( int k = 0; k < iMax - iMin; ++k ) {
				final int diff = iMin + k - i;
				corrs[k] = Math.exp( -0.5 * diff * diff / 20 );
			}
			final Meta meta = new Meta();
			meta.zPosition = i;
			meta.zCoordinateMax = iMax;
			meta.zCoordinateMin = iMin;
			sco.addCorrelationsAt( 0, 0, i, corrs, meta);
		}
		
		sco.toMatrix( 0,  0 );
		new ImageJ();
		ImageJFunctions.show( sco.toMatrix( 0, 0 ) );
		
	}

	
	@Override
	public Set<SerializableConstantPair<Long, Long>> getXYCoordinates() {
		return this.correlations.keySet();
	}
	
	
	@Override
	public <T extends RealType<T> & NativeType<T>> void toCorrelationStripe(
			final long x, final long y, final RandomAccessibleInterval< T > stripe) {
		assert stripe.numDimensions() == 2;
		// stripe should have odd dimension 0 ( range + reference + range = 2*n+1 values at each row )
		assert ( stripe.dimension( 0 ) & 1 ) == 1;
		assert stripe.dimension( 1 ) == this.zMax - this.zMin;
		for ( final T s : Views.flatIterable( stripe ) )
			s.setReal( Double.NaN );
		
		final TreeMap<Long, double[]> subMap = this.correlations.get( SerializableConstantPair.toPair( x, y ) );
		
		final long midIndex = stripe.dimension( 0 ) / 2;
		
		for ( long zRef = this.zMin, zeroBasedCount = 0; zRef < this.zMax; ++zRef, ++zeroBasedCount ) {
			final IntervalView<T> hs = Views.hyperSlice( stripe, 1, zeroBasedCount );
			final Meta meta = this.metaMap.get( zRef );
			final double[] corr = subMap.get( zRef );
			
			final RandomAccess<T> target    = hs.randomAccess();
			final Cursor<DoubleType> source = ArrayImgs.doubles( corr, corr.length ).cursor();
			
			final long relativeStart = midIndex - ( meta.zPosition - meta.zCoordinateMin );
			target.setPosition( relativeStart, 0 );
			while( source.hasNext() ) {
				target.get().setReal( source.next().get() );
				target.fwd( 0 );
			}
		}
	}

	@Override
	public long getxMin() {
			return xMin;
	}

	@Override
	public long getyMin() {
		return yMin;
	}

	@Override
	public long getxMax() {
		return xMax;
	}

	@Override
	public long getyMax() {
		return yMax;
	}


}
