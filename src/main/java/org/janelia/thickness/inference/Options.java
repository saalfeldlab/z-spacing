/**
 *
 */
package org.janelia.thickness.inference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class Options implements Serializable
{

	/**
	 *
	 */
	private static final long serialVersionUID = 3591334824905556420L;

	public static Options generateDefaultOptions()
	{
		final Options result = new Options();
		result.scalingFactorRegularizerWeight = 0.1;
		result.coordinateUpdateRegularizerWeight = 0.0;
		result.shiftProportion = 0.6;
		result.nIterations = 100;
		result.comparisonRange = 10;
		result.minimumSectionThickness = 0.01;
		result.regularizationType = InferFromMatrix.RegularizationType.BORDER;
		result.scalingFactorEstimationIterations = 10;
		result.withReorder = true;
		result.nThreads = Runtime.getRuntime().availableProcessors();
		result.forceMonotonicity = false;
		result.estimateWindowRadius = -1;
		result.minimumCorrelationValue = 0.0;
		result.estimateRegularizer = 0.0;
		result.pairwisePotentialRegularizer = 0.0;
		return result;
	}

	public Double scalingFactorRegularizerWeight; // m_regularized = m *
	// ( 1 - w ) + 1 * w

	public Double coordinateUpdateRegularizerWeight; // coordinate_regularized =
	// predicted * ( 1 - w )
	// + original * w

	public Double shiftProportion; // actual_shift = shift * shiftProportion

	public Integer nIterations; // number of iterations

	public Integer comparisonRange; // range for cross correlations

	public Double minimumSectionThickness;

	public InferFromMatrix.RegularizationType regularizationType;

	public Integer scalingFactorEstimationIterations;

	public Boolean withReorder;

	public Integer nThreads;

	public Boolean forceMonotonicity;

	public Integer estimateWindowRadius;

	public Double minimumCorrelationValue;

	public Double estimateRegularizer;

	public Double pairwisePotentialRegularizer;

	public static Options read( final String filename ) throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		final Gson gson = new Gson();
		final Options opt = gson.fromJson( new FileReader( filename ), Options.class );
		return opt;
	}

	@Override
	public Options clone()
	{
		final Options result = new Options();
		for ( final Field f : this.getClass().getDeclaredFields() )
		{
			if ( f.getName().equals( "serialVersionUID" ) )
				continue;
			try
			{
				f.set( result, f.get( this ) );
			}
			catch ( final IllegalArgumentException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append( "[" );
		sb.append( getClass().getName() );
		sb.append( "]\n" );
		for ( final Field f : this.getClass().getDeclaredFields() )
		{
			if ( f.getName().equals( "serialVersionUID" ) )
				continue;
			sb.append( f.getName() );
			sb.append( "\t" );
			try
			{
				sb.append( f.get( this ) );
			}
			catch ( final IllegalArgumentException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sb.append( "\n" );
		}

		return sb.toString();
	}

	public void toFile( final String filename ) throws IOException
	{
		final Gson gson = new Gson();
		final String json = gson.toJson( this );
		FileUtils.writeStringToFile( new File( filename ), json );
	}

	@Override
	public boolean equals( final Object other )
	{
		if ( other instanceof Options )
		{
			for ( final Field f : this.getClass().getDeclaredFields() )
			{
				if ( f.getName().equals( "serialVersionUID" ) )
					continue;
				try
				{
					if ( !f.get( this ).equals( f.get( other ) ) )
						return false;
				}
				catch ( final IllegalArgumentException e )
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				catch ( final IllegalAccessException e )
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
		else
			return false;
	}

	public static void main( final String[] args ) throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		final String fn = "/data/hanslovskyp/khaled_2014_10_24/range=5_2014-10-27 11:22:17.651999/options";
		final Options options = Options.read( fn );
		System.out.println( options.toString() );
		System.out.println( Options.generateDefaultOptions().toString() );
	}

}
