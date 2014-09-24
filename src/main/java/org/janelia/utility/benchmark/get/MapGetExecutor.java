package org.janelia.utility.benchmark.get;

import java.util.Map;

import org.janelia.utility.benchmark.ExecutionFunctor;

public class MapGetExecutor<K, V> implements ExecutionFunctor {
	
	private final Map<K, V> m;
	private final K accessIndex;
	/**
	 * @param m
	 */
	public MapGetExecutor(final Map<K, V> m, final K accessIndex) {
		super();
		this.m = m;
		this.accessIndex = accessIndex;
	}

	

	@Override
	public void run() {
		m.get( this.accessIndex );
	}
	
}
