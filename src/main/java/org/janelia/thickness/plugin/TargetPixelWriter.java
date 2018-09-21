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
