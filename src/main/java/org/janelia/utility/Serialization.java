package org.janelia.utility;

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

public class Serialization {
	
	public static < T extends Serializable > void serializeGeneric( final T object, final String target ) {
		try {
			final OutputStream file = new FileOutputStream( target );
			final OutputStream buffer = new BufferedOutputStream(file);
			final ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject( object );
			}
			finally {
				output.close();
			}
	    }  
	    catch(final IOException ex){
	      ex.printStackTrace();
	    }
	}
	
	
	public static < T extends Serializable > T deserializeGeneric( final String source, T target ) {
		try {
			final InputStream file = new FileInputStream( source );
		    final InputStream buffer = new BufferedInputStream(file);
		    final ObjectInput input = new ObjectInputStream (buffer);
		    try {
		        target = (T)input.readObject();
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
		return target;
	}

}
