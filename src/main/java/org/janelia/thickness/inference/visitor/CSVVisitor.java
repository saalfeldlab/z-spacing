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
