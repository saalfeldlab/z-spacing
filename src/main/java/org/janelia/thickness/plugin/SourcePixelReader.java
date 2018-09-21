package org.janelia.thickness.plugin;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

interface SourcePixelReader {

    double valueAt(int index);

    static SourcePixelReader forImageProcessor(ImageProcessor ip)
    {
        if ( ip instanceof ByteProcessor)
            return new ByteProcessorReader( ( ByteProcessor ) ip );
        if ( ip instanceof ShortProcessor)
            return new ShortProcessorReader( ( ShortProcessor ) ip );
        if ( ip instanceof FloatProcessor)
            return new FloatProcessorReader( ( FloatProcessor ) ip );

        throw new IllegalArgumentException("Image processor type not supported: " + ip.getClass().getName());
    }

    class ByteProcessorReader implements SourcePixelReader
    {
        private final byte[] data;

        public ByteProcessorReader( ByteProcessor p )
        {
            this.data = ( byte[] ) p.getPixels();
        }

        @Override
        public double valueAt( int index )
        {
            return this.data[ index ];
        }
    }

    class FloatProcessorReader implements SourcePixelReader
    {
        private final float[] data;

        public FloatProcessorReader( FloatProcessor p )
        {
            this.data = ( float[] ) p.getPixels();
        }

        @Override
        public double valueAt( int index )
        {
            return this.data[ index ];
        }
    }

    class ShortProcessorReader implements SourcePixelReader
    {
        private final short[] data;

        public ShortProcessorReader( ShortProcessor p )
        {
            this.data = ( short[] ) p.getPixels();
        }

        @Override
        public double valueAt( int index )
        {
            return this.data[ index ];
        }
    }
}
