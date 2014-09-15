package org.janelia.utility;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.IntegerType;

public class IntegralToIntegralConverter< A extends IntegerType< A >, B extends IntegerType< B > > implements Converter<A, B> {

	@Override
	public void convert(final A input, final B output) {
		output.setInteger( input.getIntegerLong() );
	}

}
