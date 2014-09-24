# built-in
import time

# java built-in
from java.lang import Long
from java.util import ArrayList

# ImageJ
from ij import IJ
from ij import ImagePlus
from ij import ImageStack
from ij.process import ImageConverter

# mpicbg
from mpicbg.ij.util import Filter

# imglib2
from net.imglib2.img import ImagePlusAdapter

# janelia
from org.janelia.correlations import CorrelationsObject
from org.janelia.correlations import CorrelationsObjectFactory
from org.janelia.correlations import CrossCorrelation
from org.janelia.correlations import SparseCorrelationsObjectFactory
from org.janelia.utility import ConstantPair
from org.janelia.utility.sampler import SparseXYSampler

# janelia python
from thickness import CorrelationsCreator


def computeCorrelationsFromStack( imgSource, scale, cRange, func ):
    stackSource = imgSource.getStack()
    stack       = ImageStack(int(round(imgSource.getWidth()*scale)), int(round(imgSource.getHeight()*scale)))
    for z in xrange(stackSource.getSize()):
        stack.addSlice(Filter.createDownsampled(
            stackSource.getProcessor(z+1),
            scale,
            0.5,
            0.5))
                
             
    img = ImagePlus("", stack)

    return func( img, cRange )

    
def funcDeprecated( img, cRange ):
    cc = CorrelationsCreator( img, [img.getWidth(), img.getHeight()] )
    cc.correlateAllWithinRange( cRange )
    co = CorrelationsObject( )
    stop = img.getStack().getSize()
    for i in xrange( 1, stop + 1 ):
        stackRange, interval = cc.toStackRange( i, cRange )
        out = ImagePlus('test_%02d' % i, stackRange)

        meta                = CorrelationsObject.Meta()
        meta.zPosition      = i - 1
        meta.zCoordinateMin = interval[0] - 1 
        meta.zCoordinateMax = interval[1] + 1 - 1# exclusive

        adapter = ImagePlusAdapter.wrap( out )
        co.addCorrelationImage(meta.zPosition, adapter, meta)
    return co

def funcJavaOnly( img, cRange ):
    wrappedImage = ImagePlusAdapter.wrap( img )
    samples      = ArrayList()
    samples      . add( ConstantPair.toPair( Long(0), Long(0) ) )
    sampler      = SparseXYSampler( samples )
    cf           = SparseCorrelationsObjectFactory( ImagePlusAdapter.wrap( img ), sampler )
    co           = cf.create( cRange, [img.getWidth(), img.getHeight() ] )
    return co


def funcJavaOnlyDense( img, cRange ):
    wrappedImage = ImagePlusAdapter.wrap( img )
    cf           = SparseCorrelationsObjectFactory( ImagePlusAdapter.wrap( img ), CrossCorrelation.TYPE.SIGNED_SQUARED )
    co           = cf.create( cRange, [img.getWidth(), img.getHeight() ] )
    return co




if __name__ == "__main__":
    root = '/ssd/hanslovskyp/boergens/substacks/01/'
    number = 10
    # only set this to True if the number of images as well as range is very short! calculation will take long!
    checkConsistencyWithDeprecated = True
    toColonWidth = 75
    # load iamges
    IJ.run( "Image Sequence...", "open=%s/data number=%d sort" % ( root.rstrip(), number ) )
    imgSource = IJ.getImage()
    cRange = 3

    conv = ImageConverter( imgSource )
    conv.convertToGray32()

    t0 = time.time()
    pco = computeCorrelationsFromStack( imgSource, 0.1, cRange, funcDeprecated )
    t1 = time.time()
    print '%10s%f' % ( 'jython correlations creation time:'.ljust( toColonWidth ),  t1 - t0 )

    t0 = time.time()
    sco = computeCorrelationsFromStack( imgSource, 0.1, cRange, funcJavaOnly )
    t1 = time.time()
    print '%10s%f' % ( 'sparse (one coordinate) correlations creation time:'.ljust( toColonWidth ),  t1 - t0 )

    print pco.equalsMeta( sco )

    if checkConsistencyWithDeprecated:
        t0 = time.time()
        dco = computeCorrelationsFromStack( imgSource, 0.1, cRange, funcJavaOnlyDense )
        t1 = time.time()
        print '%10s%f' % ( 'dense correlations creation time:'.ljust( toColonWidth ),  t1 - t0 )

        print pco.equalsXYCoordinates( dco )
        print pco.equals( dco )

    
