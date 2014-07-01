package org.janelia.waves.thickness.opinion;

import java.util.TreeMap;

import mpicbg.models.Tile;
import mpicbg.models.TranslationModel1D;

import org.janelia.utility.ConstantTriple;

public class OpinionMediatorModelFactory implements
		OpinionMediatorFactoryInterface {

	private final TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> tileMap;
	
	

	public OpinionMediatorModelFactory() {
		this( new TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>>() );
	}



	public OpinionMediatorModelFactory(
			TreeMap<ConstantTriple<Long, Long, Long>, Tile<TranslationModel1D>> tileMap) {
		super();
		this.tileMap = tileMap;
	}



	public OpinionMediatorInterface create(double lambda) {
	
			return new OpinionMediatorModel( lambda, this.tileMap );
	
	}
	
	

}
