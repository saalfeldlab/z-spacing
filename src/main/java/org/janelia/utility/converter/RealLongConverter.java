package org.janelia.utility;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

public class RealLongConverter< R extends RealType< R > > implements Converter< R, LongType > {

	@Override
	public void convert(final R input, final LongType output) {
		output.set( (long) input.getRealDouble() ); 
	}

}
