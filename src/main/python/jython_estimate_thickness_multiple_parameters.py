from ij import ImagePlus
from ij import ImageStack
from ij.process import ImageConverter

from java.lang import Double
from java.lang import Long
from java.lang import System

from java.util import ArrayList
from java.util import TreeMap

from mpicbg.ij.integral import BlockPMCC
from mpicbg.models import TranslationModel1D
from mpicbg.ij.util import Filter

from net.imglib2.img.array import ArrayImgs
from net.imglib2.img.display.imagej import ImageJFunctions
from net.imglib2.img import ImagePlusAdapter
from net.imglib2.img.display.imagej import ImgLib2Display
from net.imglib2.interpolation.randomaccess import NLinearInterpolatorFactory
from net.imglib2.interpolation.randomaccess import NearestNeighborInterpolatorFactory
from net.imglib2.interpolation.randomaccess import FloorInterpolatorFactory
from net.imglib2.view import Views
from net.imglib2.realtransform import RealViews
from net.imglib2.img.imageplus import ImagePlusImgs
from net.imglib2.type.numeric.real import DoubleType

from org.janelia.models import ScaleModel
from org.janelia.utility import ConstantPair
from org.janelia.utility import CopyFromIntervalToInterval
from org.janelia.correlations import CorrelationsObject
from org.janelia.thickness import InferFromCorrelationsObject
from org.janelia.thickness.lut import SingleDimensionLUTRealTransform
from org.janelia.thickness.inference.visitor import ActualCoordinatesTrackerVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImagesAndAverageVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImageVisitor
from org.janelia.thickness.inference.visitor import CorrelationArrayTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationFitTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationMatrixTrackerVisitor
from org.janelia.thickness.inference.visitor import MultipliersTrackerVisitor
from org.janelia.thickness.inference.visitor import WeightsTrackerVisitor
from org.janelia.thickness.mediator import OpinionMediatorModel



import os
import math
import time
import jarray
import errno

def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError, exception: # need comma here, 'as' does not work in jython
        if exception.errno != errno.EEXIST:
            raise

class Fitter(object):
    def __init__(self, offset, initialGuess, function, stepsize, fittingRange):
        # self.fitter       = StackFitterNoUncertainty(offset, initialGuess, function)
        self.stepSize     = stepSize
        self.fittingRange = fittingRange


    def fit(self, image):
        pass


class FitterFactory(object):
    def create(self, offset, initialGuess, function, stepsize, fittingRange):
        return Fitter(offset, initialGuess, function, stepsize, fittingRange)

    


class CorrelationsCreator(object):
    def __init__(self, imagePlus, radius=[8, 8], offset=[0,0]):
        self.correlations = {}
        self.radius = radius
        self.imagePlus = imagePlus
        self.offset = [0,0]

    def correlate(self, index1, index2):
        if (index1, index2) in self.correlations.keys():
            pass 
        elif (index2, index1) in self.correlations.keys():
            self.correlations[(index1, index2)] = self.correlations[(index2, index1)]
        else:
            self.correlations[(index1, index2)] = self.computeCorrelation(index1, index2)
        
        return self.correlations[(index1, index2)]

    def computeCorrelation(self, index1, index2):
        ip1  = self.imagePlus.getStack().getProcessor(index1).convertToFloatProcessor()
        ip2  = self.imagePlus.getStack().getProcessor(index2).convertToFloatProcessor()
        pmcc = BlockPMCC(ip1, ip2, *self.offset)
        pmcc.rSignedSquare(self.radius[0], self.radius[1])
        tp = pmcc.getTargetProcessor()
        tp.min(0.0)
        return tp
        

    def correlateAll(self):
        size = self.imagePlus.getStack().getSize()
        # for index1 in xrange(1, size+1):
        #     for index2 in xrange(1, size+1):
        #         self.correlate(index1, index2)

        return self.correlateAllWithinRange(size)

        # return self.correlations

    def correlateAllWithinRange(self, maximumRange):
        size = self.imagePlus.getStack().getSize()
        for index1 in xrange(1, size+1):
            for index2 in xrange(1, size+1):
                if int(math.fabs(index2-index1)) > maximumRange:
                    continue
                self.correlate(index1, index2)
                
        return self. correlations

    def toStack(self, index):
        size = self.imagePlus.getStack().getSize()
        width = self.imagePlus.getStack().getWidth()
        height = self.imagePlus.getStack().getHeight()
        resultStack = ImageStack(width, height, size)
        for k, v in self.correlations.iteritems():
            if k[0] == index:
                resultStack.setProcessor(v, k[1])
        return resultStack, (1, size)
        # return self.toStackRange(index, size/2)

    def toStackRange(self, index, stackRange):
        upperEnd = min(index + stackRange, self.imagePlus.getStack().getSize())
        lowerEnd = max(index - stackRange, 1)
        size = upperEnd - lowerEnd + 1 # min(self.imagePlus.getStack().getSize(), 2*stackRange+1)
        width = self.imagePlus.getStack().getWidth()
        height = self.imagePlus.getStack().getHeight()
        resultStack = ImageStack(width, height, size)
        for k, v in self.correlations.iteritems():
            if k[0] == index:
                # if int(math.fabs(k[1] - index)) > stackRange:
                # continue
                if k[1] > upperEnd or k[1] < lowerEnd:
                    continue
                putIndex = k[1] + 1 - lowerEnd
                resultStack.setProcessor(v, putIndex)
        return resultStack, (lowerEnd, upperEnd)


class Bucket(object):
    def __init__(self, image, fitterFactory, stepSize, fittingRange):
        self.buckets = {}
        self.image   = image
        self.fitterFactory  = fitterFactory
        self.stepSize = stepSize
        self.fittingRange = fittingRange

    def createBucketsAtScaleAndRange(radius, stackRange):
        co = CorrelationsCreator(self.image, radius=radius)
        co.correlateAllWithinRange(stackRange)
        result = []
        for idx in xrange(1, self.image.getStack().getSize() + 1):
            stack, interval = co.toStackRange(idx, stackRange)
            offset = idx - interval[0]
            initialGuess = [1.0]
            function = SingleParameterBellCurve()
            fitter = fitterFactory.create(offset, initialGuess, function, self.stepSize, self.fittingRange)            
            result.append(fitter.fit(stack, interval))
        self.buckets[radius] = result
        return result
            

if __name__ == "__main__":

    t0 = time.time()
    print t0 - t0
    
    # imgSource = IJ.getImage()
    # imgSource   = ImagePlus( '/groups/saalfeld/home/hanslovskyp/data/thickness/test_data/davi/intensity_corrected/crop/intensity_1_removed_slices.tif' )
    imgSource   = ImagePlus( '/groups/saalfeld/home/hanslovskyp/data/thickness/test_data/fibsem/crop/test_stack_234_8bit.tif' )
    conv = ImageConverter( imgSource )
    conv.convertToGray32()
    stackSource = imgSource.getStack()
    xyScale = 1.0
    stack = ImageStack(int(round(imgSource.getWidth()*xyScale)), int(round(imgSource.getHeight()*xyScale)))

    resultPathBase = '/ssd/hanslovskyp/playground/thickness/fibsem_test_stack_234/%02d'


    for z in xrange(stackSource.getSize()):
        stack.addSlice(Filter.createDownsampled(
            stackSource.getProcessor(z+1),
	    xyScale,
	    0.5,
            0.5))

    img = ImagePlus("", stack)

    
    
    correlationRanges = xrange(3,16,2)
    for correlationRange in correlationRanges:
        print corrleationRange
        cc = CorrelationsCreator(img, [img.getWidth(), img.getHeight()])
        cc.correlateAllWithinRange( correlationRange )
                            
                            
        options                   = CorrelationsObject.Options()
        # options.fitIntervalLength = 3
        # options.stride            = 2
        # options.fitterFactory     = StackFitterNoUncertaintyFactory([1.0])
                            
        co = CorrelationsObject(options)
                            
        coordinateBase = ArrayList()
                            
        positions = TreeMap();
        positions.put( ConstantPair( Long(0), Long(0) ), ArrayList() );
                            
        startingCoordinates = []
                            
                            
        for i in xrange( 1, img.getStack().getSize() + 1 ):
            stackRange, interval = cc.toStackRange( i, correlationRange )
                            
            out = ImagePlus('test_%02d' % i, stackRange)
            # out.show()    
                            
            meta                = CorrelationsObject.Meta()
            meta.zPosition      = i
            meta.zCoordinateMin = interval[0]
            meta.zCoordinateMax = interval[1] + 1 # exclusive
                            
            adapter = ImagePlusAdapter.wrap(out)
            co.addCorrelationImage(meta.zPosition, adapter, meta)
                            
            coordinateBase.add( float(i) )
                            
            startingCoordinates.append( float(i-1) )
                            
                            
                            
        nIterations = 50    
        nThreads = 1        
                            
        inference = InferFromCorrelationsObject( co,
                                                 nIterations,
                                                 correlationRange,
                                                 TranslationModel1D(),
                                                 NLinearInterpolatorFactory(),
                                                 ScaleModel(),
                                                 nThreads,
                                                 OpinionMediatorModel( TranslationModel1D() )
                                                 )
                                                 
        home = resultPathBase.rstrip('/') % correlationRange
                            
        bp = home + "/matrix/matrix%02d.tif"
        make_sure_path_exists(bp)
        matrixTracker = CorrelationMatrixTrackerVisitor( bp, # base path
                                                         10, # min
                                                         50, # max
                                                         20, # scale
                                                         FloorInterpolatorFactory() ) # interpolation
                            
        bp = home + "/array/array%02d.tif"
        make_sure_path_exists(bp)
        arrayTracker = CorrelationArrayTrackerVisitor( bp, # base path
                                                       FloorInterpolatorFactory(), # interpolation
                                                       imgSource.getStack().getSize(), # number of data points
                                                       correlationRange ) # range for pairwise correlations
                            
        bp = home + "/render/render%02d.tif"
        make_sure_path_exists(bp)
        hyperSlices = ArrayList()
        # hyperSlice = Views.hyperSlice( ImagePlusImgs.from( imgSource ), 1,  345 )
        # ImageJFunctions.show( hyperSlice )
        scale = 1.0         
        renderTracker = ApplyTransformToImagesAndAverageVisitor( bp, # base path
                                                                 FloorInterpolatorFactory(), # interpolation
                                                                 scale )
        for i in xrange(-5, 6, 1):
            renderTracker.addImage( Views.hyperSlice( ImagePlusImgs.from( imgSource ), 1,  250 + i ) )                                                 
                            
        bp = home + "/fit_tracker/fitTracker%d.csv"
        make_sure_path_exists(bp)
        separator = ','     
        fitTracker = CorrelationFitTrackerVisitor( bp, # base path
                                                   correlationRange, # range
                                                   separator ) # csv separator
                            
        bp = home + "/fit_coordinates/fitCoordinates%d.csv"
        make_sure_path_exists(bp)
        coordinateTracker = ActualCoordinatesTrackerVisitor( bp,
                                                             separator )
                                                             
        bp = home + "/multipliers/multipliers%d.csv"
        make_sure_path_exists(bp)
        multiplierTracker = MultipliersTrackerVisitor( bp,
                                                       separator )
                            
        bp = home + "/weights/weights%d.csv"
        make_sure_path_exists(bp)
        weightsTracker = WeightsTrackerVisitor( bp,
                                                separator )                                                                                                          
                            
        matrixTracker.addVisitor( arrayTracker )
        matrixTracker.addVisitor( renderTracker )
        matrixTracker.addVisitor( fitTracker )
        matrixTracker.addVisitor( coordinateTracker )
        matrixTracker.addVisitor( multiplierTracker )
        matrixTracker.addVisitor( weightsTracker )                                         
                                                        
        result = inference.estimateZCoordinates( 0, 0, startingCoordinates, matrixTracker )
                            
                            
        #array = jarray.zeros( result.dimension(0), 'd' )
        #cursor = result.cursor()
        #for i in xrange(result.dimension(0) ):
        #    array[i] = scale * cursor.next().get()
        #                    
        #lutTransform = SingleDimensionLUTRealTransform( array, 3, 3, 2 )
        #                    
        #resultImage = ImagePlusImgs.unsignedBytes( imgSource.getWidth(), imgSource.getHeight(), int(scale) * stack.getSize() )
        #interpolated = Views.interpolate( Views.extendValue( ImagePlusImgs.from(imgSource), DoubleType( Double.NaN ) ), FloorInterpolatorFactory() )
        #transformed =  Views.interval( RealViews.transform( interpolated, lutTransform ), resultImage )
        #                    
        #CopyFromIntervalToInterval.copy( transformed, resultImage )
                            
                            
        #ImageJFunctions.show( resultImage )
                            
 
