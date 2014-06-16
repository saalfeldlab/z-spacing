package org.janelia.waves.thickness;

import net.imglib2.Pair;

public class ConstantPair<A, B> implements Pair<A, B> {

	private final A entryA;
	private final B entryB;
	
	
	
	public ConstantPair(A entryA, B entryB) {
		super();
		this.entryA = entryA;
		this.entryB = entryB;
	}

	public A getA() {
		return entryA;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(" + entryA.toString() + "," + entryB.toString() + ")";
	}

	public B getB() {
		return entryB;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof ConstantPair<?, ?> ) {
			return ( ( ConstantPair<A, B> ) obj ).entryA.equals( this.entryA ) &&
				   ( ( ConstantPair<A, B> ) obj ).entryB.equals( this.entryB );
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return entryA.hashCode() + entryB.hashCode();
	}
	
	

}
