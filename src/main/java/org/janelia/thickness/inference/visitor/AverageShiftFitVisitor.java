/**
 *
 */
package org.janelia.thickness.inference.visitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class AverageShiftFitVisitor implements Visitor
{

	private final String path;

	private final File f;

	public AverageShiftFitVisitor( final String path ) throws IOException

	{
		this.path = path;
		this.f = new File( path );
		FileUtils.writeStringToFile( f, "" );
	}

	@Override
	public < T extends RealType< T > > void act(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final RandomAccessibleInterval< T > scaledMatrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFits,
			final double averageShift )
	{
		try
		{
			FileUtils.writeStringToFile( f, averageShift + "\n", Charset.defaultCharset(), true );
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
