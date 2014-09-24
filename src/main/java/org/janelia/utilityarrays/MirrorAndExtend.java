package org.janelia.utilityarrays;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class MirrorAndExtend {
	
	public static RealRandomAccessible< DoubleType > doubles(
			 final double[] data,
             final InterpolatorFactory< DoubleType, RandomAccessible< DoubleType > > interpolatorFactory )
     {
             final ArrayImg< DoubleType, DoubleArray > img = ArrayImgs.doubles( data, new long[]{ data.length } );
             final RandomAccessible< DoubleType > mirror = Views.extendMirrorSingle( img );
             final RandomAccessibleInterval< DoubleType > crop = Views.interval( mirror, new long[]{ 1 - data.length }, new long[]{ data.length - 1 } );
             final RandomAccessible< DoubleType > extension = Views.extendValue( crop, new DoubleType( Double.NaN ) );
             return Views.interpolate( extension, interpolatorFactory );
     }

}
