package org.janelia.thickness.inference.fits;

import mpicbg.models.PointMatch;

import java.util.ArrayList;

/**
 * Created by hanslovskyp on 10/7/15.
 */
public class CorrelationFitAverage extends AbstractCorrelationFit {
    @Override
    protected double estimate(ArrayList<PointMatch> measurements) {
        double sum = 0.0;
        double weightSum = 0.0;
        for( PointMatch m : measurements )
        {
            double weight = m.getWeight();
            sum += weight*m.getP2().getW()[0];
            weightSum += weight;
        }
        return sum / weightSum;
    }
}
