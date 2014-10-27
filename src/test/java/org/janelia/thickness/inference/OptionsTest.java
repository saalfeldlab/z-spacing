/**
 * 
 */
package org.janelia.thickness.inference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
	}

}
