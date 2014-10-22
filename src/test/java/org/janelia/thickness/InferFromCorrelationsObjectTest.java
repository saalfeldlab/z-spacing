package org.janelia.thickness;

import org.janelia.thickness.InferFromCorrelationsObject.Options;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InferFromCorrelationsObjectTest {
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
