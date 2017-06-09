package org.janelia.thickness.inference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.janelia.thickness.inference.fits.GlobalCorrelationFitAverage;
import org.janelia.thickness.inference.visitor.AverageShiftFitVisitor;
import org.janelia.thickness.inference.visitor.LUTVisitor;
import org.janelia.thickness.inference.visitor.ListVisitor;
import org.janelia.thickness.inference.visitor.MatrixVisitor;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class GridSearch
{

	public static void main( final String[] args ) throws Exception
	{

		final int nIterations = 2000;

		final String path = "/data/hanslovskyp/forPhilipp/substacks/03/matrix-chopped.tif";

		final RandomAccessibleInterval< FloatType > matrix = ImageJFunctions.wrapReal( new ImagePlus( path ) );

//		ImageJFunctions.show( matrix );

		final ExecutorService es = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() - 1 );


		final ArrayList< Callable< Void > > tasks = new ArrayList<>();

		for ( double shiftProportion = 0.0; shiftProportion <= 4.0; shiftProportion += 1.0 )
			for ( double reg = 0.0; reg <= 2.0; reg += 0.5 )
			{
				final Options opts = Options.generateDefaultOptions();
				opts.shiftProportion = shiftProportion;
				opts.pairwisePotentialRegularizer = reg;
				opts.nIterations = nIterations;
				opts.withReorder = true;
				opts.scalingFactorEstimationIterations = 0;
				opts.comparisonRange = 30;
				opts.forceMonotonicity = true;
				opts.minimumSectionThickness = 1e-9;
				opts.regularizationType = InferFromMatrix.RegularizationType.BORDER;
				final double[] startingCoordinates = IntStream.range( 0, ( int ) matrix.dimension( 0 ) ).mapToDouble( i -> i ).toArray();
				final ListVisitor vis = new ListVisitor();
				final String basePath = String.format( System.getProperty( "user.home" ) + "/z-spacing-gridsearch-chopped/%.1f-%.1f", shiftProportion, reg );
				final LUTVisitor lutVisitor = new LUTVisitor( basePath, "", "," );
				lutVisitor.setRelativeFilePattern( "lut/", opts.nIterations, ".csv" );
				final MatrixVisitor matrixVisitor = new MatrixVisitor( basePath, "", opts.comparisonRange );
				matrixVisitor.setRelativeFilePattern( "matrices/", opts.nIterations, ".tif" );
				vis.addVisitor( lutVisitor );
				vis.addVisitor( new AverageShiftFitVisitor( basePath + "/average-shifts/shift" ) );
				vis.addVisitor( matrixVisitor );
				final InferFromMatrix inf = new InferFromMatrix( new GlobalCorrelationFitAverage() );
				tasks.add( () -> {
					inf.estimateZCoordinates( matrix, startingCoordinates, vis, opts );
					return null;
				} );
			}

		final List< Future< Void > > futures = es.invokeAll( tasks );
		for ( final Future< Void > f : futures )
			f.get();

		es.shutdown();

	}

}
