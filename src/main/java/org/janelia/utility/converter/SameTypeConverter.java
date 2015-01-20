/**
 * 
 */
package org.janelia.utility;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.NumericType;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SameTypeConverter<T extends NumericType<T>> implements
		Converter< T, T > {

	@Override
	public void convert(final T input, final T output) {
		output.set( input );
	}

}
