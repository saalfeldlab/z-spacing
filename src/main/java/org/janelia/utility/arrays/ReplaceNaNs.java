package org.janelia.utility.arrays;

import java.util.Arrays;

/**
 * Created by hanslovskyp on 10/24/15.
 */
public class ReplaceNaNs {

    public static void replace( final double[] array )
    {
        int lastElement = array.length - 1;
        double currentLower = array[0];

        if( Double.isNaN( currentLower ) )
        {
            for( int d = 1; d < array.length; ++d )
            {
                double val = array[d];
                if (!Double.isNaN( val ) ) {
                    currentLower = val - d;
                    array[0] = currentLower;
                    break;
                }
            }
        }

        for( int d = 0; d < array.length; ++d )
        {
            double val = array[d];
            if ( Double.isNaN( val ) ) {
                final int start = d;
                int currentLowerIndex = start - 1;
                double currentUpper = array[d];
                while ( Double.isNaN( currentUpper ) )
                {
                    ++d;
                    if ( d == array.length )
                    {
                        if ( Double.isNaN( currentUpper ) )
                            currentUpper = currentLower + d + - currentLowerIndex;
                        break;
                    }
                    currentUpper = array[d];
                }
                double normalize = 1.0/(d + - currentLowerIndex);
                double diff = currentUpper - currentLower;
                for ( int s = start; s < d; ++s) {
                    array[s] = (s - start + 1) * normalize * diff + currentLower;
                }
                if ( d < array.length )
                    currentLower = array[d];

            } else
                currentLower = val;
        }
    }

    public static void main(String[] args) {
        double[] array = new double[]{ Double.NaN, Double.NaN, 3, Double.NaN, Double.NaN, 6, Double.NaN, 8, 9, Double.NaN };
        System.out.println( Arrays.toString(array) );
        replace( array );
        System.out.println( Arrays.toString(array) );
    }

}
