package org.janelia.waves.thickness;

public class ConstantTriple<A, B, C> implements Comparable< ConstantTriple< A, B, C> > {
	private final A entryA;
	private final B entryB;
	private final C entryC;
	
	
	
	public ConstantTriple(A entryA, B entryB, C entryC) {
		super();
		this.entryA = entryA;
		this.entryB = entryB;
		this.entryC = entryC;
	}

	public A getA() {
		return entryA;
	}

	public B getB() {
		return entryB;
	}

	/**
	 * @return the entryC
	 */
	public C getC() {
		return entryC;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof ConstantPair<?, ?> ) {
			return ( ( ConstantTriple<A, B, C> ) obj ).entryA.equals( this.entryA ) &&
				   ( ( ConstantTriple<A, B, C> ) obj ).entryB.equals( this.entryB ) &&
				   ( ( ConstantTriple<A, B, C> ) obj ).entryC.equals( this.entryC );
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return entryA.hashCode() + entryB.hashCode() + entryC.hashCode();
	}

	@SuppressWarnings("unchecked")
	public int compareTo(ConstantTriple<A, B, C> o) {
		if ( !( entryA instanceof Comparable<?> ) || 
			 !( entryB instanceof Comparable<?> ) ||
			 !( entryC instanceof Comparable<?> ) ) {
			return 0;
		}
		if ( ( (Comparable<A>) entryA ).compareTo( o.entryA ) == 0 ) {
			
			if ( ( (Comparable<B>) entryB ).compareTo( o.entryB ) == 0 ) {
				return ( (Comparable<C>) entryC ).compareTo( o.entryC );
			} else {
				return ( (Comparable<B>) entryB ).compareTo( o.entryB );
			}
			
		} else { 
			return ( (Comparable<A>) entryA ).compareTo( o.entryA ); 
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(" + entryA.toString() + "," + entryB.toString() + "," + entryC.toString() +")";
	}
	
	

}

