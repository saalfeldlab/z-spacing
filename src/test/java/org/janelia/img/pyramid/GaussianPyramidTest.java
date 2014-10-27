package org.janelia.img.pyramid;

import ij.ImageJ;
import ij.ImagePlus;

import java.io.IOException;
import java.util.Random;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Before;
import org.junit.Test;

public class GaussianPyramidTest {
	
	private final Random rng = new Random( 100 );
	private final Img< DoubleType > img = ArrayImgs.doubles( 200, 100, 3 );

	@Before
	public void setUp() throws Exception {
		for ( final DoubleType i : img )
			i.set( rng.nextDouble() );
	}

	@Test
	public void test() throws IOException {
		final ImagePlus original  = new ImagePlus( "http://d3sdoylwcs36el.cloudfront.net/VEN-virtual-enterprise-network-business-opportunities-small-fish_id799929_size485.jpg" );
		final ImagePlus converted = new ImagePlus( "", original.getProcessor().convertToFloat() );
		final FloatImagePlus<FloatType> wrapped = ImagePlusAdapter.wrapFloat( converted );
		new ImageJ();
		ImageJFunctions.show( wrapped );
		final GaussianPyramid<FloatType> pyr = new GaussianPyramid<FloatType>( wrapped, 0.5, 2.0, new ArrayImgFactory<FloatType>());
		for ( int index = 0; index < 2; ++index ) { // pyr.getMaxLevel(); ++index ) {
			ImageJFunctions.show( pyr.get( index ) );
		}
	}

}
