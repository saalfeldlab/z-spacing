/*-
 * #%L
 * Z spacing plugin for Fiji.
 * %%
 * Copyright (C) 2014 - 2023 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.janelia.thickness.plugin;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

interface TargetPixelWriter {
    void setValueAt(int index, double value);

    static TargetPixelWriter forImageProcessor(ImageProcessor ip)
    {
        if ( ip instanceof ByteProcessor)
            return new ByteProcessorWriter( ( ByteProcessor ) ip );
        if ( ip instanceof ShortProcessor)
            return new ShortProcessorWriter( ( ShortProcessor ) ip );
        if ( ip instanceof FloatProcessor)
            return new FloatProcessorWriter( ( FloatProcessor ) ip );

        throw new IllegalArgumentException( "Image processor type not supported: " + ip.getClass().getName() );
    }

    class ByteProcessorWriter implements TargetPixelWriter
    {
        private final byte[] data;

        public ByteProcessorWriter( ByteProcessor p )
        {
            this.data = ( byte[] ) p.getPixels();
        }

        @Override
        public void setValueAt( int index, double value )
        {
            this.data[ index ] = ( byte ) ( value + 0.5 );
        }
    }

    class FloatProcessorWriter implements TargetPixelWriter
    {
        private final float[] data;

        public FloatProcessorWriter( FloatProcessor p )
        {
            this.data = ( float[] ) p.getPixels();
        }

        @Override
        public void setValueAt( int index, double value )
        {
            this.data[ index ] = ( float ) value;
        }
    }

    class ShortProcessorWriter implements TargetPixelWriter
    {
        private final short[] data;

        public ShortProcessorWriter( ShortProcessor p )
        {
            this.data = ( short[] ) p.getPixels();
        }

        @Override
        public void setValueAt( int index, double value )
        {
            this.data[ index ] = ( short ) ( value + 0.5 );
        }
    }
}
