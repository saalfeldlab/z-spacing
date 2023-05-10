/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2023 Howard Hughes Medical Institute.
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
