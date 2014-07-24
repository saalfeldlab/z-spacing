package org.janelia.waves.thickness.opinion;

import org.janelia.waves.thickness.opinion.OpinionMediatorWeightedMean.TYPE;

public class OpinionMediatorWeightedMeanFactory implements
		OpinionMediatorFactoryInterface {
	
	private final OpinionMediatorWeightedMean.TYPE type;
	
	

	public OpinionMediatorWeightedMeanFactory(TYPE type) {
		super();
		this.type = type;
	}



	public OpinionMediatorInterface create(double lambda) {
		switch( this.type ) {
	
		case ARITHMETIC:
			return new OpinionMediatorWeightedMean( new OpinionMediatorWeightedMean.Arithmetic() );
		
		case GEOMETRIC:
			return new OpinionMediatorWeightedMean( new OpinionMediatorWeightedMean.Geometric() );
	
		case HARMONIC:
			return new OpinionMediatorWeightedMean( new OpinionMediatorWeightedMean.Harmonic() );
			
		default:
			return new OpinionMediatorWeightedMean();
		}
	}

}
