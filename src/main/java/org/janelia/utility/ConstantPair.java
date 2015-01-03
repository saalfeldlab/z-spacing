package org.janelia.utility;

import java.io.Serializable;

import net.imglib2.util.Pair;


public class ConstantPair<A, B> implements Pair<A, B>, Comparable< ConstantPair< A, B> >, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7963474267757313817L;
	private final A entryA;
	private final B entryB;



	public ConstantPair(final A entryA, final B entryB) {
		super();
		this.entryA = entryA;
		this.entryB = entryB;
	}

	@Override
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

	@Override
	public B getB() {
		return entryB;
	}

//	/* (non-Javadoc)
//	 * @see java.lang.Object#equals(java.lang.Object)
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public boolean equals(Object obj) {
//		if ( obj instanceof ConstantPair<?, ?> ) {
//			return ( (org.janelia.utility.ConstantPair<A, B> ) obj ).entryA.equals( this.entryA ) &&
//				   ( (org.janelia.utility.ConstantPair<A, B> ) obj ).entryB.equals( this.entryB );
//		}
//		return false;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return entryA.hashCode() + entryB.hashCode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(final ConstantPair<A, B> o) {
		if ( !( entryA instanceof Comparable<?> ) ||
		     !( entryB instanceof Comparable<?> )) {
				return 0;
			}
			if ( ( (Comparable<A>) entryA ).compareTo( o.entryA ) == 0 ) {
				return ( (Comparable<B>) entryB ).compareTo( o.entryB );
			} else {
				return ( (Comparable<A>) entryA ).compareTo( o.entryA );
			}
	}


	public static < U , B > ConstantPair< U, B > toPair( final U u, final B b ) {
		return new ConstantPair<U, B >( u, b );
	}



}
