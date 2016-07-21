/**
 * 
 */
package org.janelia.thickness.inference.visitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class MultipliersTrackerVisitor extends AbstractMultiVisitor
{

	private final String basePath;

	private final String separator;

	private int r;

	public MultipliersTrackerVisitor( final String basePath, final String separator )
	{
		this( new ArrayList< Visitor >(), basePath, separator );
	}

	public MultipliersTrackerVisitor( final ArrayList< Visitor > visitors, final String basePath, final String separator )
	{
		super( visitors );
		this.basePath = basePath;
		this.separator = separator;
	}

	@Override
	< T extends RealType< T > > void actSelf(
			final int iteration,
			final RandomAccessibleInterval< T > matrix,
			final double[] lut,
			final int[] permutation,
			final int[] inversePermutation,
			final double[] multipliers,
			final RandomAccessibleInterval< double[] > estimatedFit )
	{

		final File file = new File( String.format( this.basePath, iteration ) );
		r = 0;
		try
		{

			file.createNewFile();
			final FileWriter fw = new FileWriter( file.getAbsoluteFile() );
			final BufferedWriter bw = new BufferedWriter( fw );

			for ( int i = 0; i < multipliers.length; ++i )
			{
				final double c = multipliers[ inversePermutation[ i ] ];
				bw.write( String.format( "%d" + this.separator + "%f\n", ++r, c ) );
			}

			bw.close();
		}
		catch ( final IOException e )
		{
			// catch exceptions?
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
