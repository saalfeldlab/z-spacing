/**
 * 
 */
package org.janelia.utility.tuple;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SerializableConstantPair< A extends Serializable, B extends Serializable > extends ConstantPair<A, B> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4356793553420213701L;
	
	public SerializableConstantPair(final A entryA, final B entryB) {
		super( entryA, entryB );
	}

	
	public static < U extends Serializable, B extends Serializable > SerializableConstantPair< U, B > toPair( final U u, final B b ) {
		return new SerializableConstantPair<U, B >( u, b );
	}
	
	
	public static void main(final String[] args) {
		
		final SerializableConstantPair<Integer, Integer> p1 = toPair( new Integer(2), new Integer(3) );
		final String source = System.getProperty( "user.dir" ) + "/pair.sr";
		SerializableConstantPair<Integer, Integer> target = new SerializableConstantPair<Integer, Integer>(1, 1);
		try {
			final OutputStream file = new FileOutputStream( source );
			final OutputStream buffer = new BufferedOutputStream(file);
			final ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject( p1 );
			}
			finally {
				output.close();
			}
	    }  
	    catch(final IOException ex){
	      
	    }
		try {
			final InputStream file = new FileInputStream( source );
		    final InputStream buffer = new BufferedInputStream(file);
		    final ObjectInput input = new ObjectInputStream (buffer);
		    try {
		        target = (SerializableConstantPair<Integer, Integer> )input.readObject();
		    }
		    finally{
		    	input.close();
		    }
		}
		catch(final ClassNotFoundException ex){
			ex.printStackTrace();
		}
		catch(final IOException ex){
			ex.printStackTrace();
		}
		
		System.out.println( target.getA() + " " + target.getB() );
	}
	
}
	
	



