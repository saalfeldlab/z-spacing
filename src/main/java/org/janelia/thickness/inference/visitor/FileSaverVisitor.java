package org.janelia.thickness.inference.visitor;

import java.io.File;

/**
 *
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public abstract class FileSaverVisitor implements Visitor
{

	protected String basePath;

	protected String relativeFilePattern;

	public String getBasePath()
	{
		return basePath;
	}

	public void setBasePath( final String basePath )
	{
		this.basePath = basePath;
	}

	public String getRelativeFilePattern()
	{
		return relativeFilePattern;
	}

	public void setRelativeFilePattern( final String relativeFilePattern )
	{
		this.relativeFilePattern = relativeFilePattern;
	}

	public void setRelativeFilePattern( final String base, final int maxIteration, final String extension )
	{
		final int length = String.valueOf( maxIteration ).length();
		this.relativeFilePattern = String.format( "%s%%0%dd%s", base, Math.max( length, 4 ), extension );
	}


	public FileSaverVisitor( final String basePath, final String relativeFilePattern )
	{
		this.basePath = basePath;
		this.relativeFilePattern = relativeFilePattern;
	}

	protected String fileDir( final int iteration )
	{
		return String.format( "%s/%s", basePath, String.format( relativeFilePattern, iteration ) );
	}

	protected void createParentDirectory( final String path )
	{
		new File( path ).getParentFile().mkdirs();
	}


}
