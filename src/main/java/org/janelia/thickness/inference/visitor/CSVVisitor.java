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
package org.janelia.thickness.inference.visitor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.scijava.util.FileUtils;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public abstract class CSVVisitor extends FileSaverVisitor
{

	public static class IndexedIterable< T > implements Iterable< String >
	{

		private final String separator;

		private final Iterable< T > iterable;

		public IndexedIterable( final String separator, final Iterable< T > iterable )
		{
			this.separator = separator;
			this.iterable = iterable;
		}

		public class IndexedIterator implements Iterator< String >
		{

			private final Iterator< T > it = iterable.iterator();

			private int index = -1;

			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

			@Override
			public String next()
			{
				return String.format( "%d%s%s", ++index, separator, it.next().toString() );
			}

		}

		@Override
		public Iterator< String > iterator()
		{
			return new IndexedIterator();
		}

	}

	protected String separator;

	public String getSeparator()
	{
		return separator;
	}

	public void setSeparator( final String separator )
	{
		this.separator = separator;
	}

	public CSVVisitor( final String basePath, final String relativeFilePattern, final String separator )
	{
		super( basePath, relativeFilePattern );
		this.separator = separator;
	}

	protected < T > void write( final Iterable< T > source, final String path ) throws IOException
	{
		final StringBuilder sb = new StringBuilder( "" );
		final Iterator< T > it = source.iterator();
		if ( it.hasNext() )
			sb.append( it.next() );
		while ( it.hasNext() )
			sb.append( "\n" ).append( it.next() );
		FileUtils.writeFile( new File( path ), sb.toString().getBytes() );
	}

}
