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

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.shear.AbstractShearTransform;
import net.imglib2.transform.integer.shear.ShearTransform;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class MatrixStripConversion
{

	public static < T extends RealType< T > > RandomAccessibleInterval< T > stripToMatrix(
			final RandomAccessibleInterval< T > strip )
	{
		T extension = Util.getTypeFromInterval( strip ).createVariable();
		extension.setReal( Double.NaN );
		return stripToMatrix( strip, extension );
	}

	public static < T extends Type< T > > RandomAccessibleInterval< T > stripToMatrix(
			final RandomAccessibleInterval< T > strip,
			final T extension )
	{
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( strip, extension );
//		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( Views.expandBorder( strip, 1, 1 ), dummy );
		final AbstractShearTransform tf = new ShearTransform( 2, 0, 1 ).inverse();
		final long w = strip.dimension( 0 ) / 2;
		final long h = strip.dimension( 1 );
		final FinalInterval interval = new FinalInterval( new long[] { w, 0 }, new long[] { h + w - 1, h - 1 } );
		final IntervalView< T > transformed = Views.offsetInterval( new TransformView<>( extended, tf ), interval );
		return transformed;
	}

	public static < T extends RealType< T > > RandomAccessibleInterval< T > matrixToStrip(
			final RandomAccessibleInterval< T > matrix,
			final int range )
	{
		T extension = Util.getTypeFromInterval( matrix ).createVariable();
		extension.setReal( Double.NaN );
		return matrixToStrip( matrix, range, extension );
	}

	public static < T extends Type< T > > RandomAccessibleInterval< T > matrixToStrip(
			final RandomAccessibleInterval< T > matrix,
			final int range,
			final T extension )
	{
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extended = Views.extendValue( matrix, extension );
		final AbstractShearTransform tf = new ShearTransform( 2, 0, 1 );
		final long h = matrix.dimension( 1 );
		final FinalInterval interval = new FinalInterval( new long[] { -range, 0 }, new long[] { range, h - 1 } );
		final IntervalView< T > transformed = Views.offsetInterval( new TransformView<>( extended, tf ), interval );
		return transformed;
	}
}
