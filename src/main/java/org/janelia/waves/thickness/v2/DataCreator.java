package org.janelia.waves.thickness.v2;

import ij.IJ;
import ij.ImageJ;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;

import org.janelia.waves.thickness.functions.imglib.FunctionRandomAccessible1D;
import org.janelia.waves.thickness.functions.symmetric.BellCurve;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.TranslationModel1D;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class DataCreator {
	
	public static interface BinShifter {
		
		public ArrayImg<DoubleType, DoubleArray> shift( ArrayImg<DoubleType, DoubleArray> bins );
		
	}
	
	public static class ZeroShifter implements BinShifter {

		public ArrayImg<DoubleType, DoubleArray> shift(
				ArrayImg<DoubleType, DoubleArray> bins) {
			return bins;
		}
		
	}
	
	public static class OneSliceEffectsFollowingShifter implements BinShifter {
		
		private final int shiftBin;
		private final double shift;
		

		public OneSliceEffectsFollowingShifter(int shiftBin, double shift) {
			super();
			this.shiftBin = shiftBin;
			this.shift    = shift;
		}


		public ArrayImg<DoubleType, DoubleArray> shift(
				ArrayImg<DoubleType, DoubleArray> bins) {
			@SuppressWarnings("unchecked")
			ArrayImg<DoubleType, DoubleArray> result = (ArrayImg<DoubleType, DoubleArray>) bins.copy();
			
			
			int binsToShiftLeft = (int) (result.dimension( 0 ) - shiftBin);
			int binsToShift     = binsToShiftLeft;
			
			for ( int bin = shiftBin; bin < result.dimension( 0 ); ++ bin ) {
				ArrayRandomAccess<DoubleType> ra = result.randomAccess();
				ra.setPosition( bin, 0 );
				ra.get().set( binsToShiftLeft / (double) binsToShift * shift + ra.get().get() ); 
				binsToShiftLeft -= 1;
			}
			
			return result;
		}
		
	}
	
	
	public static class AccumulativeShifter implements BinShifter {
		
		private final Random rng;
		private final double std;
		private final double mean;
		private final double threshold;
		private final double limit;
		
		public AccumulativeShifter(double std, double mean, double threshold, double limit, long seed) {
			super();
			this.std = std;
			this.mean = mean;
			this.threshold = threshold;
			this.limit = limit;
			rng = new Random( seed );
		}
		public ArrayImg<DoubleType, DoubleArray> shift(
				ArrayImg<DoubleType, DoubleArray> bins) {
			ArrayImg<DoubleType, DoubleArray> result = (ArrayImg<DoubleType, DoubleArray>) bins.copy();
			
			double accumulatedShift = 0.0;
			
			for ( DoubleType b : result ) {
//				double currentShift = Math.abs( rng.nextGaussian()*this.std + this.mean );
				double currentShift = Math.abs( rng.nextDouble() * this.std );
				
				if ( currentShift > threshold && currentShift < limit ) {
					accumulatedShift += currentShift;
				}
				b.set( b.get() + accumulatedShift );
			}
			
			
			return result;
		}
		
	}
	
	public static interface CorrelationDegrader {
		
		public ArrayImg< DoubleType, DoubleArray > degrade( ArrayImg< DoubleType, DoubleArray > correlations );
		
	}
	
	public static class IdentityDegrader implements CorrelationDegrader {

		public ArrayImg<DoubleType, DoubleArray> degrade(
				ArrayImg<DoubleType, DoubleArray> correlations) {
			return correlations;
		}
		
	}
	
	public static class MultiplierDegrader implements CorrelationDegrader {
		
		private final long[] bins;
		private final double[] multipliers;

		public MultiplierDegrader(long[] bins, double[] multipliers) {
			super();
			this.bins = bins;
			this.multipliers = multipliers;
		}

		public ArrayImg<DoubleType, DoubleArray> degrade(
				ArrayImg<DoubleType, DoubleArray> correlations) {
			
			ArrayCursor<DoubleType> cursor = correlations.cursor();
			
			DoubleType value = null;
			while ( cursor.hasNext() ) {
				
				value = cursor.next();
				if ( cursor.getLongPosition( CorrelationsObjectToArrayImg.DZ_AXIS ) == ( correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) / 2 ) ) {
					continue;
				}
				long position = cursor.getIntPosition( CorrelationsObjectToArrayImg.Z_AXIS ) + ( cursor.getIntPosition( CorrelationsObjectToArrayImg.DZ_AXIS ) - correlations.dimension( CorrelationsObjectToArrayImg.DZ_AXIS ) / 2 );
				int index = -1;
				for ( int i = 0; i < this.bins.length; ++i ) {
					if ( cursor.getLongPosition( CorrelationsObjectToArrayImg.Z_AXIS ) == this.bins[i] || position == this.bins[i] ) {
						index = i;
						break;
					}
				}
				if ( index > -1 ) {
					value.mul( this.multipliers[index] );
				}
			}
			return correlations;
		}
		
	}
	
	
	public static class RandomMultiplierDegrader implements CorrelationDegrader {
		
		private final Random rng = new Random(100);
		
		public ArrayImg<DoubleType, DoubleArray> degrade(
				ArrayImg<DoubleType, DoubleArray> correlations) {
			
			long[] wh = new long[ 2 ];
			correlations.dimensions( wh );
			
			ArrayRandomAccess<DoubleType> ra = correlations.randomAccess();
			
			for (int i = 0; i < correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ); i++) {
				double multiplier = Math.max(0.0, 1 - Math.abs( rng.nextGaussian()*0.1 )  );
				for ( DoubleType c: Views.flatIterable(Views.hyperSlice(correlations, CorrelationsObjectToArrayImg.Z_AXIS, i))) {
					c.set( c.get() * multiplier );
				}
				
				ra.setPosition( i, CorrelationsObjectToArrayImg.Z_AXIS );
				ra.setPosition( wh[ 0 ] / 2, CorrelationsObjectToArrayImg.DZ_AXIS );
				ra.fwd( CorrelationsObjectToArrayImg.Z_AXIS );
				ra.bck( CorrelationsObjectToArrayImg.DZ_AXIS );
				while ( ra.getLongPosition( CorrelationsObjectToArrayImg.DZ_AXIS ) >= 0 && ra.getLongPosition( CorrelationsObjectToArrayImg.Z_AXIS ) < wh[ 1 ] ) {
					ra.get().mul(multiplier);
					ra.fwd( CorrelationsObjectToArrayImg.Z_AXIS );
					ra.bck( CorrelationsObjectToArrayImg.DZ_AXIS );
				}
				ra.setPosition( i, CorrelationsObjectToArrayImg.Z_AXIS );
				ra.setPosition( wh[ 0 ] / 2, CorrelationsObjectToArrayImg.DZ_AXIS );
				ra.bck( CorrelationsObjectToArrayImg.Z_AXIS );
				ra.fwd( CorrelationsObjectToArrayImg.DZ_AXIS );
				while ( ra.getLongPosition( CorrelationsObjectToArrayImg.DZ_AXIS ) < wh[ 0 ] && ra.getLongPosition( CorrelationsObjectToArrayImg.Z_AXIS ) >= 0 ) {
					ra.get().mul(multiplier);
					ra.bck( CorrelationsObjectToArrayImg.Z_AXIS );
					ra.fwd( CorrelationsObjectToArrayImg.DZ_AXIS );
				}
			}
			return correlations;
		}
		
	}
	
	private final int nZBins;
	private final int correlationRange;
	private final RealRandomAccessible< DoubleType > correlationsCreator;
	private final BinShifter binShifter;
	private final ArrayImg< DoubleType, DoubleArray > zCoordinates;
	
	
	public DataCreator(int nZBins, int correlationRange,
			RealRandomAccessible<DoubleType> correlationsCreator,
			BinShifter binShifter) {
		super();
		this.nZBins = nZBins;
		this.correlationRange = correlationRange;
		this.correlationsCreator = correlationsCreator;
		this.binShifter = binShifter;
		
		double[] bins = new double[ this.nZBins ];
	
		for (int i = 0; i < bins.length; ++i) {
			bins[i] = i;
		}
		
		this.zCoordinates = this.binShifter.shift( ArrayImgs.doubles( bins, bins.length ) );
		
	}
	
	
	public ArrayImg< DoubleType, DoubleArray > create( CorrelationDegrader degrader ) {
		double[] correlationsArray                    = new double[ ( 2*correlationRange + 1 ) * nZBins ];
		for (int i = 0; i < correlationsArray.length; ++i) {
			correlationsArray[i] = Double.NaN;
		}
		ArrayImg<DoubleType, DoubleArray> correlations = ArrayImgs.doubles( correlationsArray, 2*correlationRange + 1, nZBins );
		ArrayRandomAccess<DoubleType> randomAccess     = correlations.randomAccess();
		RealRandomAccess<DoubleType> corrAccess        = correlationsCreator.realRandomAccess();
		ArrayRandomAccess<DoubleType> coordAccess      = this.zCoordinates.randomAccess();
		
		
		for ( int zBin = 0; zBin < this.nZBins; ++zBin ) {
			randomAccess.setPosition( zBin, CorrelationsObjectToArrayImg.Z_AXIS );
			coordAccess.setPosition( zBin, 0 );
			double zReference = coordAccess.get().getRealDouble();
			for ( int dz = -this.correlationRange; ( dz <= this.correlationRange ) && ( zBin + dz < this.nZBins ) ; ++dz ) {
				if ( zBin + dz < 0 ) {
					continue;
				}
				coordAccess.setPosition( zBin + dz, 0 );
				corrAccess.setPosition( coordAccess.get().getRealDouble() - zReference, 0 );
				randomAccess.setPosition( dz + this.correlationRange, CorrelationsObjectToArrayImg.DZ_AXIS );
				randomAccess.get().set( corrAccess.get().getRealDouble() );
			}
			
		}
		
		return degrader.degrade( correlations );
	}
	
	
	public static void writeToFile( ArrayImg< DoubleType, DoubleArray > correlations, String filename, String delimiter, int precision ) throws IOException {
		
		
		
		String formatString            = "%." + precision + "f";
		ArrayCursor<DoubleType> cursor = correlations.cursor();
		
		Writer writer = null;
		writer = new BufferedWriter( new OutputStreamWriter(
		          new FileOutputStream(filename), "utf-8") );
		while ( cursor.hasNext() ) {
			double value = cursor.next().get();
			String writeString = String.format( formatString, value ) + delimiter;
			if ( cursor.getIntPosition( 0 ) == correlations.dimension( 0 ) - 1 ) {
				writeString = String.format( formatString, value ) + "\n";
			}
			writer.write( writeString );
				
		}
		writer.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		
		int nZBins = 300;
		int nCorrelations = 50;
		
		DataCreator creator = null;
		ArrayImg<DoubleType, DoubleArray> correlations = null;
		
//		creator = new DataCreator( nZBins, nCorrelations, new FunctionRandomAccessible1D( new double[] {0.0, 30.0}, new BellCurve() ), new ZeroShifter() );
//		correlations = creator.create( new IdentityDegrader() );
//		writeToFile( correlations, "zero_shift_no_degrade.csv", ",", 5);
//		
//		creator = new DataCreator( nZBins, nCorrelations, new FunctionRandomAccessible1D( new double[] {0.0, 30.0}, new BellCurve() ), new OneSliceEffectsFollowingShifter( 40, 4.0 ) );
//		correlations = creator.create( new IdentityDegrader() );
//		writeToFile( correlations, "4.0_shift_no_degrade.csv", ",", 5);
//		
//		creator = new DataCreator( nZBins, nCorrelations, new FunctionRandomAccessible1D( new double[] {0.0, 30.0}, new BellCurve() ), new OneSliceEffectsFollowingShifter( 40, 4.0 ) );
//		correlations = creator.create( new MultiplierDegrader( new long[] { 35, 80}, new double[] { 0.8, 0.5} ) );
//		writeToFile( correlations, "4.0_shift_degrade.csv", ",", 5);
		
//		creator = new DataCreator( nZBins, nCorrelations, new FunctionRandomAccessible1D( new double[] {0.0, 30.0}, new BellCurve() ), new AccumulativeShifter( 5.0, 0.0, 0.0, 10, 100) );
//		correlations = creator.create( new MultiplierDegrader( new long[] { 35, 80}, new double[] { 0.8, 0.5} ) );
//		writeToFile( correlations, "accumulated_shift_degrade.csv", ",", 5);
		
		creator = new DataCreator( nZBins, nCorrelations, new FunctionRandomAccessible1D( new double[] {0.0, 30.0}, new BellCurve() ), new AccumulativeShifter( 5.0, 0.0, 0.0, 10, 100) );
		correlations = creator.create( new RandomMultiplierDegrader() );
		writeToFile( correlations, "accumulated_shift_degrade.csv", ",", 5);
		
		double[] weightsArray = new double[ (int) correlations.dimension( CorrelationsObjectToArrayImg.Z_AXIS ) ];
		for (int i = 0; i < weightsArray.length; i++) {
			weightsArray[i] = 1.0;
		}
		ArrayImg<DoubleType, DoubleArray> weights = ArrayImgs.doubles( weightsArray, weightsArray.length );
		TranslationModel1D model = new TranslationModel1D();
		ArrayImg<DoubleType, DoubleArray> estimate = null;
		try {
			estimate = EstimateCorrelationsAtSamplePoints.estimate( correlations, weights, model);
		} catch (NotEnoughDataPointsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllDefinedDataPointsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ( estimate != null ) {
			for ( DoubleType e : estimate ) {
				System.out.print( e.getRealDouble() + " " );
			}
			System.out.println();
		}
		
		new ImageJ();
		
		ImageJFunctions.show( Views.permute(correlations, 0, 1) );
		IJ.getImage().getProcessor().setMinAndMax(0, 1);
		IJ.getImage().updateAndDraw();
	}

}
