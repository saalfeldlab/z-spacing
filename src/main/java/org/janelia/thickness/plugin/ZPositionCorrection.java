package org.janelia.thickness.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.janelia.thickness.inference.InferFromMatrix;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.inference.fits.AbstractCorrelationFit;
import org.janelia.thickness.inference.fits.GlobalCorrelationFitAverage;
import org.janelia.thickness.inference.fits.LocalCorrelationFitAverage;
import org.janelia.thickness.inference.visitor.CorrelationFitVisitor;
import org.janelia.thickness.inference.visitor.FileSaverVisitor;
import org.janelia.thickness.inference.visitor.LUTVisitor;
import org.janelia.thickness.inference.visitor.LazyVisitor;
import org.janelia.thickness.inference.visitor.ListVisitor;
import org.janelia.thickness.inference.visitor.ScalingFactorsVisitor;
import org.janelia.thickness.inference.visitor.Visitor;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.thickness.lut.PermutationTransform;
import org.janelia.thickness.lut.SingleDimensionLUTRealTransform;
import org.janelia.thickness.lut.SingleDimensionPermutationTransform;
import org.janelia.utility.MatrixStripConversion;
import org.janelia.utility.arrays.ArraySortedIndices;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.FolderOpener;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageConverter;
import mpicbg.ij.util.Filter;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.transform.Transform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TransformView;
import net.imglib2.view.Views;

/**
 * @author Philipp Hanslovsky &lt;hanslovskyp@janelia.hhmi.org&gt;
 *
 */
public class ZPositionCorrection implements PlugIn
{

	private static HashMap< String, VisitorFactory > visitors = new HashMap<>();
	static
	{
		addVisitor( "lazy", new LazyVisitor() );

		final VisitorFactory factory = new VisitorFactory()
		{
			@Override
			public Visitor create( final RandomAccessibleInterval< DoubleType > matrix, final Options options )
			{
				final GenericDialogPlus dialog = new GenericDialogPlus( "Choose output directory for visitor!" );
				dialog.addDirectoryField( "Output directory", System.getProperty( "user.home" ) );
				dialog.showDialog();
				if ( dialog.wasCanceled() )
					return new LazyVisitor();

				final String basePath = dialog.getNextString();

				final ListVisitor lv = new ListVisitor();
				lv.addVisitor( new CorrelationFitVisitor( "", "", ",", 0 ) );
				lv.addVisitor( new ScalingFactorsVisitor( "", "", "," ) );
				lv.addVisitor( new LUTVisitor( "", "", "," ) );

				final ArrayList< Visitor > vs = lv.getVisitors();
				final String[] relativePatternsBase = {
						"correlation-fit/", "scaling-factors/", "lut/"
				};
				for ( int i = 0; i < relativePatternsBase.length; ++i )
				{
					final FileSaverVisitor v = ( FileSaverVisitor ) vs.get( i );
					v.setRelativeFilePattern( relativePatternsBase[ i ], options.nIterations, ".csv" );
					v.setBasePath( basePath );
				}
				return lv;
			}
		};
		addVisitor( "variables", factory );
	}

	public static interface VisitorFactory
	{
		Visitor create( final RandomAccessibleInterval< DoubleType > matrix, final Options options );
	}

	public static void addVisitor( final String name, final Visitor visitor )
	{
		addVisitor( name, ( final RandomAccessibleInterval< DoubleType > matrix, final Options options ) ->
		{
			return visitor;
		} );
	}

	public static void addVisitor( final String name, final VisitorFactory factory )
	{
		if ( name.equals( "lazy" ) && visitors.containsKey( name ) )
			IJ.log( "Default visitor (\"lazy\") will not be replaced." );
		else
			visitors.put( name, factory );
	}

	@Override
	public void run( final String arg0 )
	{

		final Options options = Options.generateDefaultOptions();

		final GenericDialogPlus dialog = new GenericDialogPlus( "Correct layer z-positions" );
		dialog.addMessage( "Data source settings : " );
		dialog.addFileField( "Input path (use current image if empty)", "" );
		dialog.addChoice( "Type of input data : ", new String[] { "Matrix", "Image Stack" }, "Image Stack" );
		dialog.addMessage( "Inference settings : " );
		dialog.addMessage( "Section neighbor range :" );
		dialog.addNumericField( "test_maximally :", options.comparisonRange, 0, 6, "layers" );
		dialog.addMessage( "Optimizer :" );
		dialog.addNumericField( "outer_iterations :", options.nIterations, 0, 6, "" );
		dialog.addNumericField( "outer_regularization :", 1.0 - options.shiftProportion, 2, 6, "" );
		dialog.addNumericField( "inner_iterations :", options.scalingFactorEstimationIterations, 0, 6, "" );
		dialog.addNumericField( "inner_regularization :", options.scalingFactorRegularizerWeight, 2, 6, "" );
		dialog.addCheckbox( " allow_reordering", options.withReorder );
		dialog.addNumericField( "number of local estimates :", 1, 0, 6, "" );

		synchronized ( visitors )
		{
			final String[] visitorStrings = new String[ visitors.size() ];
			final Iterator< String > keysIterator = visitors.keySet().iterator();
			for ( int i = 0; i < visitorStrings.length; ++i )
				visitorStrings[ i ] = keysIterator.next();

			dialog.addChoice( "Visitor", visitorStrings, visitors.containsKey( "lazy" ) ? "lazy" : visitorStrings[ 0 ] );
		}

		dialog.showDialog();

		if ( dialog.wasCanceled() )
			return;

		final String inputPath = dialog.getNextString();
		final boolean inputIsMatrix = dialog.getNextChoiceIndex() == 0;
		final ImagePlus input = inputPath.equals( "" ) ? IJ.getImage() : FolderOpener.open( inputPath );

		options.comparisonRange = ( int ) dialog.getNextNumber();
		options.nIterations = ( int ) dialog.getNextNumber();
		options.shiftProportion = 1.0 - dialog.getNextNumber();
		options.scalingFactorEstimationIterations = ( int ) dialog.getNextNumber();
		options.scalingFactorRegularizerWeight = dialog.getNextNumber();
		options.withReorder = dialog.getNextBoolean();
		options.forceMonotonicity = !options.withReorder;
		options.minimumSectionThickness = 1e-9;
		options.regularizationType = InferFromMatrix.RegularizationType.BORDER;

		final int nLocalEstimates = ( int ) dialog.getNextNumber();

		final String visitorString = dialog.getNextChoice();

		final FloatProcessor matrixFp = inputIsMatrix ? normalize( input ).getProcessor().convertToFloatProcessor() : calculateSimilarityMatrix( input, options.comparisonRange );

		if ( matrixFp == null )
			return;

		final boolean isStrip = matrixFp.getWidth() != matrixFp.getHeight();
		final RandomAccessibleInterval< DoubleType > wrappedFp = wrapDouble( new ImagePlus( "", matrixFp ) );

		final RandomAccessibleInterval< DoubleType > matrix = isStrip ? MatrixStripConversion.stripToMatrix( wrappedFp, new DoubleType() ) : wrappedFp;

		if ( !inputIsMatrix )
			ImageJFunctions.show( matrix );

		final double[] startingCoordinates = new double[ ( int ) matrix.dimension( 0 ) ];
		for ( int i = 0; i < startingCoordinates.length; i++ )
			startingCoordinates[ i ] = i;

		options.estimateWindowRadius = startingCoordinates.length / nLocalEstimates;
		final AbstractCorrelationFit correlationFit = nLocalEstimates < 2 ? new GlobalCorrelationFitAverage() : new LocalCorrelationFitAverage( startingCoordinates.length, options );
		final InferFromMatrix inf = new InferFromMatrix( correlationFit );

		boolean estimatedSuccessfully = false;
		double[] transform = null;
		try
		{
			final VisitorFactory factory = visitors.get( visitorString );
			final Visitor visitor = factory.create( matrix, options );
			transform = inf.estimateZCoordinates( matrix, startingCoordinates, visitor, options );
			estimatedSuccessfully = true;
		}
		catch ( final NotEnoughDataPointsException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final IllDefinedDataPointsException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		System.out.println( options.toString() );

		if ( estimatedSuccessfully )
		{

			IJ.log( Arrays.toString( transform ) );
			boolean renderTransformedStack = false;

			final double[] sortedTransform = transform.clone();
			final int[] forward = new int[ sortedTransform.length ];
			final int[] backward = new int[ sortedTransform.length ];

			if ( options.withReorder )
				ArraySortedIndices.sort( sortedTransform, forward, backward );
			else
				for ( int i = 0; i < forward.length; ++i )
				{
					forward[ i ] = i;
					backward[ i ] = i;
				}

			final int[] permutationArray = backward; // use backward?

			final PermutationTransform permutation = new PermutationTransform( permutationArray, 2, 2 );
			final LUTRealTransform lut = new LUTRealTransform( sortedTransform, 2, 2 );
			final IntervalView< DoubleType > transformedStripOrMatrix = Views.interval( Views.raster(
					generateTransformed( matrix, permutation, lut, new DoubleType( Double.NaN ) ) ), matrix );
			final RandomAccessibleInterval< DoubleType > transformedMatrix = isStrip ? MatrixStripConversion.matrixToStrip( transformedStripOrMatrix, options.comparisonRange, new DoubleType() ) : transformedStripOrMatrix;

			ImageJFunctions.show( transformedMatrix, "Warped matrix" );

			double renderingZScale = 1.0;
			final GenericDialogPlus renderDialog = new GenericDialogPlus( "Rendering." );
			renderDialog.addCheckbox( "Render stack?", renderTransformedStack );
			if ( inputIsMatrix )
				renderDialog.addFileField( "Input path (use current image if empty)", "" );
			renderDialog.addNumericField( "Scale result stack", renderingZScale, 4 );
			renderDialog.showDialog();

			if ( renderDialog.wasCanceled() )
				return;

			renderTransformedStack = renderDialog.getNextBoolean();
			renderingZScale = renderDialog.getNextNumber();

			if ( renderTransformedStack )
			{
				final ImagePlus stackImp = inputIsMatrix ? getFileFromOption( renderDialog.getNextString() ) : input;
				new ImageConverter( stackImp ).convertToGray32();
				final RandomAccessibleInterval< FloatType > stack = ImageJFunctions.wrapFloat( stackImp );

				final SingleDimensionPermutationTransform permutation1D = new SingleDimensionPermutationTransform( permutationArray, 3, 3, 2 );
				final SingleDimensionLUTRealTransform lut1D = new SingleDimensionLUTRealTransform( sortedTransform, 3, 3, 2 );

				final ImageStack transformedStack =
						generateStack(
								generateTransformed( stack, permutation1D, lut1D, new FloatType( Float.NaN ) ),
								stackImp.getWidth(),
								stackImp.getHeight(),
								stackImp.getStackSize(),
								renderingZScale );

				final ImagePlus resultImp = new ImagePlus( "Warped image stack", transformedStack );
				resultImp.show();

				IJ.log( "Rendered warped image stack." );
			}

		}

	}

	public static RandomAccessibleInterval< DoubleType > wrapDouble( final ImagePlus input )
	{
		return new ConvertedRandomAccessibleInterval< FloatType, DoubleType >( ImageJFunctions.wrapFloat( input ), new RealDoubleConverter< FloatType >(), new DoubleType() );
	}

	public static ImagePlus normalize( final ImagePlus input )
	{
		final FloatProcessor fp = input.getProcessor().convertToFloatProcessor();
		final FloatStatistics stat = new FloatStatistics( fp );
		final float[] array = ( float[] ) fp.getPixels();
		for ( int i = 0; i < array.length; ++i )
			array[ i ] /= stat.max;
		return input;
	}

	public static RandomAccessibleInterval< DoubleType > normalizeAndWrap( final ImagePlus input )
	{
		return wrapDouble( normalize( input ) );
	}

	public static FloatProcessor calculateSimilarityMatrix( final ImagePlus input, final int range )
	{
		final GenericDialog dialog = new GenericDialog( "Choose similiarity calculation method" );
		dialog.addChoice( "Similarity_method :", new String[] { "NCC (aligned)" }, "NCC (aligned)" );
		dialog.showDialog();

		if ( dialog.wasCanceled() )
			return null;

		final int method = dialog.getNextChoiceIndex();
		final FloatProcessor matrix = createEmptyMatrix( input.getStack().getSize() );

		boolean similarityCalculationWasSuccessful = false;
		switch ( method )
		{
		case 1:
			similarityCalculationWasSuccessful = invokeSIFT( input, range, matrix ); // not
			// implemented
			// yet
		default:
			similarityCalculationWasSuccessful = invokeNCC( input, range, matrix );
		}
		if ( similarityCalculationWasSuccessful )
			return matrix;
		else
			return null;
	}

	public static void main( final String[] args )
	{
		new ImageJ();
//		ImagePlus imp = new ImagePlus("/data/hanslovskyp/davi_toy_set/substacks/shuffle/03/data/data.tif");
//		ImagePlus imp = new FolderOpener().openFolder( "/data/hanslovskyp/forPhilipp/substacks/03/data/" );
//		ImagePlus imp = new FolderOpener().openFolder( "/data/hanslovskyp/davi_toy_set/data/seq" );
		final ImagePlus imp = new ImagePlus( "/data/hanslovskyp/strip-example-small.tif" );
		imp.show();
		new ZPositionCorrection().run( "" );
	}

	public static boolean invokeSIFT( final ImagePlus input, final int range, final FloatProcessor matrix )
	{
		// TODO IMPLEMENT
		return false;
	}

	public static boolean invokeNCC( final ImagePlus input, final int range, final FloatProcessor matrix )
	{
		new ImageConverter( input ).convertToGray32();
		final ImageStack stackSource = input.getStack();

		final GenericDialog dialog = new GenericDialog( "NCC options" );
		dialog.addNumericField( "Scale xy before similarity calculation", 1.0, 3 );
		dialog.showDialog();
		if ( dialog.wasCanceled() )
			return false;

		final double xyScale = dialog.getNextNumber();

		final ImageStack stack = xyScale == 1.0 ? stackSource : downsampleStack( stackSource, xyScale );
		final int height = input.getStackSize();
		final int nThreads = Runtime.getRuntime().availableProcessors();
		final ArrayList< Callable< Void > > callables = new ArrayList< Callable< Void > >();
		for ( int i = 0; i < height; ++i )
		{
			final int finalI = i;
			callables.add( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					for ( int k = finalI + 1; k - finalI <= range && k < height; ++k )
					{
						final float val = new RealSumFloatNCC( ( float[] ) stack.getProcessor( finalI + 1 ).getPixels(), ( float[] ) stack.getProcessor( k + 1 ).getPixels() ).call().floatValue();
						matrix.setf( finalI, k, val );
						matrix.setf( k, finalI, val );
					}
					return null;
				}
			} );
		}
		final ExecutorService es = Executors.newFixedThreadPool( nThreads );
		try
		{
			es.invokeAll( callables );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static ImageStack downsampleStack( final ImageStack stackSource, final double xyScale )
	{
		final ImageStack stack = new ImageStack( ( int ) Math.round( stackSource.getWidth() * xyScale ), ( int ) Math.round( stackSource.getHeight() * xyScale ) );
		for ( int z = 1; z <= stackSource.getSize(); ++z )
			stack.addSlice( Filter.createDownsampled(
					stackSource.getProcessor( z ),
					xyScale,
					0.5f,
					0.5f ) );
		return stack;
	}

	public static FloatProcessor createEmptyMatrix( final int height )
	{
		final FloatProcessor matrix = new FloatProcessor( height, height );
		matrix.add( Double.NaN );
		for ( int i = 0; i < height; ++i )
			matrix.setf( i, i, 1.0f );
		return matrix;
	}

	public static ImagePlus getFileFromOption( final String path )
	{
		return path.equals( "" ) ? IJ.getImage() : ( new File( path ).isDirectory() ? FolderOpener.open( path ) : new ImagePlus( path ) );
	}

	public static < T extends RealType< T > > RealRandomAccessible< T > generateTransformed(
			final RandomAccessibleInterval< T > input,
			final Transform permutation,
			final InvertibleRealTransform lut,
			final T dummy )
	{
		dummy.setReal( Double.NaN );
		final IntervalView< T > permuted = Views.interval( new TransformView< T >( input, permutation ), input );
		final RealRandomAccessible< T > interpolated = Views.interpolate( Views.extendValue( permuted, dummy ), new NLinearInterpolatorFactory< T >() );
		return RealViews.transformReal( interpolated, lut );
	}

	public static < T extends RealType< T > > ImageStack generateStack(
			final RealRandomAccessible< T > input,
			final int width,
			final int height,
			final int size,
			final double zScale )
	{
		final Scale3D scaleTransform = new Scale3D( 1.0, 1.0, zScale );
		final RealTransformRealRandomAccessible< T, InverseRealTransform > scaledInput = RealViews.transformReal( input, scaleTransform );
		final int scaledSize = ( int ) ( size * zScale );
		return generateStack( scaledInput, width, height, scaledSize );
	}

	public static < T extends RealType< T > > ImageStack generateStack(
			final RealRandomAccessible< T > input,
			final int width,
			final int height,
			final int size )
	{
		final ImageStack stack = new ImageStack( width, height, size );
		final int nThreads = Runtime.getRuntime().availableProcessors();
		final ExecutorService es = Executors.newFixedThreadPool( nThreads );
		final ArrayList< Callable< Void > > callables = new ArrayList< Callable< Void > >();
		for ( int z = 0; z < size; ++z )
		{
			final int zeroBased = z;
			final int oneBased = z + 1;
			final RandomAccess< T > access = Views.raster( input ).randomAccess();

			access.setPosition( zeroBased, 2 ); // set z

			callables.add( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					final FloatProcessor fp = new FloatProcessor( width, height );
					for ( int x = 0; x < width; ++x )
					{
						access.setPosition( x, 0 );
						for ( int y = 0; y < height; ++y )
						{
							access.setPosition( y, 1 );
							fp.setf( x, y, access.get().getRealFloat() );
						}
					}
					stack.setProcessor( fp, oneBased );
					return null;
				}
			} );
		}

		try
		{
			es.invokeAll( callables );
		}
		catch ( final InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return stack;
	}

}
