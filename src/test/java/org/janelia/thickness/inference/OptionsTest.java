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
/**
 *
 */
package org.janelia.thickness.inference;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class OptionsTest {

	/**
	 * @throws java.lang.Exception
	 */
	Options baseOptions = Options.generateDefaultOptions();

	@Before
	public void setUp() throws Exception {
		baseOptions.comparisonRange = -50;
	}

	@Test
	public void test() {
		final Options defaultOptions = Options.generateDefaultOptions();
		final Options derivedOptions = baseOptions.clone();
		Assert.assertFalse( derivedOptions == defaultOptions );
		Assert.assertFalse( derivedOptions == baseOptions );
		Assert.assertFalse( defaultOptions == baseOptions );

		Assert.assertEquals( baseOptions, derivedOptions );
		Assert.assertNotEquals( baseOptions, defaultOptions );

		final String fn = "options.test";
		try {
			derivedOptions.toFile( fn );
		}
		catch ( final IOException e )
		{
			Assert.fail( "Was not able to write file " + fn );
		}
		try
		{
			Options readOptions = Options.read( fn );
			Assert.assertEquals( derivedOptions, readOptions);
			Assert.assertNotEquals( readOptions, defaultOptions );
		}
		catch ( JsonSyntaxException | JsonIOException | FileNotFoundException e )
		{
			Assert.fail( "Was not able to read file " + fn );
		}
	}

}
