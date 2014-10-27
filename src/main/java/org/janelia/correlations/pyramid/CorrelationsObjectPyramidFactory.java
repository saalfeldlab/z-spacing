package org.janelia.correlations.pyramid;

import ij.IJ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.correlations.CorrelationsObject;
import org.janelia.correlations.CorrelationsObjectFactory;
import org.janelia.correlations.CorrelationsObjectInterface;
import org.janelia.correlations.CorrelationsObjectInterface.Meta;
import org.janelia.correlations.CrossCorrelationFactoryInterface;
import org.janelia.img.pyramid.GaussianPyramid;
import org.janelia.img.pyramid.PyramidInterface;
import org.janelia.utility.ConstantPair;
import org.janelia.utility.sampler.DenseXYSampler;

public class CorrelationsObjectPyramidFactory< T extends RealType< T > > {
	
	private final RandomAccessibleInterval< T > images;
	private final CrossCorrelationFactoryInterface< T, T, FloatType > ccFactory;
	
	/**
	 * @param images
	 */
	public CorrelationsObjectPyramidFactory( final RandomAccessibleInterval<T> images, final CrossCorrelationFactoryInterface< T, T, FloatType > ccFactory ) {
		super();
		this.images = images;
		this.ccFactory = ccFactory;
	}



	public CorrelationsObjectPyramid create( final int range, final long[] radius, final int[] levels, final double scale, final boolean forceCoarsestLevel, final boolean forceFinestLevel ) {
		
		final long maxZValue = this.images.dimension( 2 ) - 1;
		
		final ConstantPair<TreeMap<Long, RandomAccessibleInterval<FloatType>>, TreeMap<Long, Meta>> correlationsAndMeta = 
				CorrelationsObjectFactory.createCorrelationsMetaPair( images, range, radius, new DenseXYSampler( images.dimension( 0 ), images.dimension( 1 ) ), ccFactory );
		final TreeMap<Long, RandomAccessibleInterval<FloatType>> highResCorrelations = correlationsAndMeta.getA();
		final TreeMap<Long, Meta> metaMap = correlationsAndMeta.getB();
		
		final ArrayList<PyramidInterface<FloatType>> correlationPyramids = new ArrayList< PyramidInterface< FloatType > >();
		for ( final Entry<Long, RandomAccessibleInterval<FloatType>> entry : highResCorrelations.entrySet() ) {
			// need to calculate sigma properly: Math.sqrt( scale )?
			IJ.log( "creating pyramid at z=" + entry.getKey() );
			final GaussianPyramid<FloatType> pyr = new GaussianPyramid<FloatType>( entry.getValue(), scale, Math.sqrt( scale ), new ArrayImgFactory<FloatType>());
			correlationPyramids.add( pyr );
		}

		final int maximumLevel;
		if ( correlationPyramids.size() > 0 )
			maximumLevel = correlationPyramids.get( 0 ).getMaxLevel();
		else
			maximumLevel = 0;
		
		final TreeSet<Integer> uniqueLevels = new TreeSet< Integer >();
		
		for ( int i = 0; i < levels.length; ++i ) {
			if ( levels[i] <= maximumLevel )
				uniqueLevels.add( levels[i] );
		}
		
		if ( forceCoarsestLevel )
			uniqueLevels.add( maximumLevel );
		
		if ( forceFinestLevel )
			uniqueLevels.add( 0 );
		
		// go through array in reverse, coarsest level should be at zero in correlations
		final Iterator<Integer> l = uniqueLevels.descendingIterator();
		
		final TreeMap<Integer, CorrelationsObject> correlationsObjectsMap = new TreeMap< Integer, CorrelationsObject >();
		
		while ( l.hasNext() ) {
			final int currentLevel = l.next();
			IJ.log( "creating at level " + currentLevel );
			final CorrelationsObject co = new CorrelationsObject();
			
			for ( int z = 0; z < correlationPyramids.size(); ++z ) {
				final RandomAccessibleInterval< FloatType > corrs;
				// create 1x1 image for coarsest level
				if ( forceCoarsestLevel && currentLevel == maximumLevel ) {
					final RandomAccessibleInterval<FloatType> orig = correlationPyramids.get( z ).get( currentLevel );
					final long zRange = orig.dimension( 2 );
					corrs = ArrayImgs.floats( 1, 1, zRange );
					final Cursor<FloatType> c = Views.flatIterable( Views.hyperSlice( Views.hyperSlice( corrs, 1, 0 ), 0, 0) ).cursor();
					for ( int index = 0; c.hasNext(); ++index ) {
						double sum = 0.0;
						final Cursor<FloatType> source = Views.flatIterable( Views.hyperSlice( orig, 2, index ) ).cursor();
						int count = 0;
						while( source.hasNext() ) {
							sum += source.next().getRealDouble();
							++count;
						}
						c.next().setReal( sum / count );
					}
				}
				else
					corrs = correlationPyramids.get( z ).get( currentLevel );
				co.addCorrelationImage( z, corrs, metaMap.get( (long)z ) );
			}
			
			correlationsObjectsMap.put( currentLevel, co );
			
		}
		
		return new CorrelationsObjectPyramid( new ArrayList< CorrelationsObjectInterface >( correlationsObjectsMap.values() ) );
	}

}
