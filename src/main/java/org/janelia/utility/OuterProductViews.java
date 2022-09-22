/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.janelia.utility;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.DoubleType;

public class OuterProductViews
{

	public static < T extends NumericType< T > > OuterProductView< T, T, T > product( final RandomAccessibleInterval< T > a1, final RandomAccessibleInterval< T > a2 )
	{
		final T t = a1.randomAccess().get().createVariable();
		return new OuterProductView<>( a1, a2, new TypeIdentity<>(), new TypeIdentity<>(), t );
	}

	public static OuterProductView< ?, ?, DoubleType > product( final double[] arr1, final double[] arr2 )
	{
		return product( ArrayImgs.doubles( arr1, arr1.length ), ArrayImgs.doubles( arr2, arr2.length ) );
	}

}
