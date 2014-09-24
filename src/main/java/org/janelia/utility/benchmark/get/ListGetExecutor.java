package org.janelia.utility.benchmark.get;

import java.util.List;

import org.janelia.utility.benchmark.ExecutionFunctor;

public class ListGetExecutor< T > implements ExecutionFunctor {
	
	private final List<T> al;
	private final int accessIndex;

	/**
	 * @param al
	 */
	public ListGetExecutor(final List<T> al, final int accessIndex) {
		super();
		assert accessIndex < al.size(): "Access index must be present in list";
		this.al          = al;
		this.accessIndex = accessIndex;
	}

	@Override
	public void run() {
		al.get( accessIndex );
	}
	
}