package org.janelia.utility.benchmark.get;

import java.util.ArrayList;
import java.util.TreeMap;

import org.janelia.utility.benchmark.Benchmark;

public class GetBenchmark {
	
	public static void main(final String[] args) {
		
		final ArrayList<Integer> al       = new ArrayList< Integer >();
		final TreeMap<Integer, Integer> m = new TreeMap< Integer, Integer >();
		
		final int maxN = 1000;
		final int accessIndex = maxN / 2;
		
		for ( int i = 0; i < maxN; ++i ) {
			al.add( i );
			m.put( i, i );
		}
		
		final ListGetExecutor<Integer> listExecutor        = new ListGetExecutor< Integer >(al, accessIndex);
		final MapGetExecutor<Integer, Integer> mapExecutor = new MapGetExecutor< Integer, Integer >( m, accessIndex);
		
		final Benchmark listBenchmark = new Benchmark( listExecutor );
		final Benchmark mapBenchmark  = new Benchmark( mapExecutor );
		
		final int nWarmupIterations = 100000;
		final int nRepetitions      = 1000;
		final int nIterations       = 1000000;
		
		listBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		mapBenchmark.evaluate(nWarmupIterations, nRepetitions, nIterations);
		
		System.out.println( listBenchmark.getMean() + " " + listBenchmark.getVar() );
		System.out.println( mapBenchmark.getMean()  + " " + mapBenchmark.getVar() );
		
	}
	
}
