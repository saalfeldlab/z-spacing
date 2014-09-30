from __future__ import with_statement # need this for with statement

from ij import ImagePlus
from ij import ImageStack
from ij import IJ
from ij.plugin import FolderOpener
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
from org.janelia.utility import SerializableConstantPair
from org.janelia.utility import Serialization
from org.janelia.utility.sampler import SparseXYSampler
from org.janelia.correlations import CorrelationsObject
from org.janelia.correlations import CorrelationsObjectFactory
from org.janelia.correlations import SparseCorrelationsObject
from org.janelia.correlations import SparseCorrelationsObjectFactory
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


import datetime
import errno
import inspect
import jarray
import math
import os
import time
import shutil
import sys

import utility

def make_sure_path_exists(path):
    try:
        os.makedirs( os.path.dirname( path ) )
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


def parseOptionsFromFile( filename ):

    defaultValues = {
        
    }
    
    result = InferFromCorrelationsObject.Options()
    visitorOptions = {}

if __name__ == "__main__":

    # import optparse

    # parser = optparse.OptionParser()
    # parser.add_option('--config', '-c', default='/dev/null', help='Config file that stores parameters and filenames.')

    # options, args = parser.parse_args()


    t0 = time.time()
    print t0 - t0

    correlationRanges = range( 54, 1001, 222221 )
    nImages = 600
    # root = '/data/hanslovskyp/playground/pov-ray/constant_thickness=5/850-1149/scale/0.05/250x250+125+125'
    # root = '/data/hanslovskyp/export_from_nobackup/sub_stack_01/data/substacks/01/'
    # root = '/ssd/hanslovskyp/crack_from_john/substacks/03/'
    # root = '/ssd/hanslovskyp/forPhilipp/substacks/03/'
    # root = '/ssd/hanslovskyp/boergens/substacks/01/'
    # root = '/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/'
    root = '/data/hanslovskyp/forPhilipp/substacks/03/'
    # root = '/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/'
    IJ.run("Image Sequence...", "open=%s/data number=%d sort" % ( root.rstrip(), nImages ) );
    # imgSource = FolderOpener().open( '%s/data' % root.rstrip('/') )
    imgSource = IJ.getImage()
    # imgSource.show()
    conv = ImageConverter( imgSource )
    conv.convertToGray32()
    stackSource = imgSource.getStack()
    nThreads = 1
    scale = 1.0
    # stackMin, stackMax = ( None, 300 )
    xyScale = 0.25 # fibsem (crack from john) ~> 0.25
    xyScale = 0.1 # fibsem (crop from john) ~> 0.1?
    doXYScale = False
    matrixSize = nImages
    matrixScale = 2.0
    serializeCorrelations = False
    deserializeCorrelations = not serializeCorrelations
    options = InferFromCorrelationsObject.Options.generateDefaultOptions()
    options.shiftProportion = 0.8
    options.nIterations = 1000
    options.nThreads = nThreads
    options.windowRange = 500
    thickness_estimation_repo_dir = '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation'
    
   

    img = imgSource.clone()
    print stackSource.getSize()
    if doXYScale:
        stack = ImageStack(int(round(imgSource.getWidth()*xyScale)), int(round(imgSource.getHeight()*xyScale)))
        for z in xrange(stackSource.getSize()):
            stack.addSlice(Filter.createDownsampled(
                stackSource.getProcessor(z+1),
    	        xyScale,
    	        0.5,
                0.5))
                
             
        img = ImagePlus("", stack)
    else:
        img = imgSource

    # img.show()
    print img.getWidth(), img.getHeight(), img.getStack().getSize()
    for c in correlationRanges:    
        correlationRange = c
        homeScale = root.rstrip('/') + '/xyScale=%f' % xyScale
        home = homeScale.rstrip('/') + '/range=%d_%s'.rstrip('/')
        home = home % ( correlationRange, str(datetime.datetime.now() ) )
        make_sure_path_exists( home.rstrip('/') + '/' )

        options.comparisonRange = c

        serializationString = '%s/correlations_range=%d.sr' % ( homeScale.rstrip(), correlationRange )

        gitCommitInfoFile = '%s/commitHash' % home.rstrip('/')
        with open( gitCommitInfoFile, 'w' ) as f:
            f.write( '%s\n' % utility.gitcommit.getCommit( thickness_estimation_repo_dir ) )

        gitDiffFile = '%s/gitDiff' % home.rstrip('/')
        with open( gitDiffFile, 'w' ) as f:
            f.write( '%s\n' % utility.gitcommit.getDiff( thickness_estimation_repo_dir ) )


        optionsFile = '%s/options' % home.rstrip('/')
        with open( optionsFile, 'w' ) as f:
            f.write( '%s\n' % options.toString() )
        

        this_file_name = os.path.realpath( inspect.getfile( lambda : None ) ) # inspect.getfile requires method, class, ... as input and returns the file in which input was defined
        shutil.copyfile( this_file_name, '%s/%s' % ( home.rstrip('/'), this_file_name.split('/')[-1] ) )
        
        # START DEPRECATED correlation calculation
        # cc = CorrelationsCreator(img, [img.getWidth(), img.getHeight()])
        # cc.correlateAllWithinRange( correlationRange )
             
        # t1 = time.time()
        # print t1 - t0
             
        # # options.coordinateUpdateRegularizerWeight = 0.1
        # # options.fitIntervalLength = 3
        # # options.stride            = 2
        # # options.fitterFactory     = StackFitterNoUncertaintyFactory([1.0])
             
        # co = CorrelationsObject()
             
        # coordinateBase = ArrayList()
             
        # positions = TreeMap();
        # positions.put( ConstantPair( Long(0), Long(0) ), ArrayList() );
             
        startingCoordinates = []
             
        # t2 = time.time()
        # print t2 - t0

        start = 1
        stop  = img.getStack().getSize()
        if False and stackMin != None:
            start = stackMin
        if False and stackMax != None:
            stop  = stackMax
        startingCoordinates = range( start - 1, stop )
        # for i in xrange( start, stop + 1 ):
        #     stackRange, interval = cc.toStackRange( i, correlationRange )
             
        #     out = ImagePlus('test_%02d' % i, stackRange)
        #     # out.show()
             
        #     meta                = CorrelationsObject.Meta()
        #     meta.zPosition      = i
        #     meta.zCoordinateMin = interval[0]
        #     meta.zCoordinateMax = interval[1] + 1 # exclusive
             
        #     adapter = ImagePlusAdapter.wrap(out)
        #     co.addCorrelationImage(meta.zPosition, adapter, meta)
             
        #     coordinateBase.add( float(i) )
             
        #     startingCoordinates.append( float(i-1) )
        # END DEPRECATED correlation calculation

        # START create CorrelationsObject with factory

        co = SparseCorrelationsObject()
        t0Prime = time.time()
        if deserializeCorrelations:
            co = Serialization.deserializeGeneric( serializationString, co )
        else:
            
        
            samples = ArrayList()
            samples.add( SerializableConstantPair.toPair( Long(0), Long(0) ) )
            sampler = SparseXYSampler( samples )
            cf      = SparseCorrelationsObjectFactory( ImagePlusAdapter.wrap( img ), sampler )
            co      = cf.create( correlationRange, [img.getWidth(), img.getHeight()] )
            Serialization.serializeGeneric( co, serializationString )
        
        t3 = time.time()
        print t3 - t0Prime

        inference = InferFromCorrelationsObject( co,
                                                 TranslationModel1D(),
                                                 NLinearInterpolatorFactory(),
                                                 ScaleModel(),
                                                 OpinionMediatorModel( TranslationModel1D() )
                                                 )
                                                 
             
             
        bp = home + "/matrix/matrix_%02d.tif"
        make_sure_path_exists( bp )
        matrixTracker = CorrelationMatrixTrackerVisitor( bp, # base path
                                                         0, # min
                                                         1000, # max
                                                         1, # scale
                                                         FloorInterpolatorFactory() ) # interpolation
             
        bp = home + "/matrix_nlinear/matrixNLinear_%02d.tif"
        make_sure_path_exists( bp )
        matrixTracker = CorrelationMatrixTrackerVisitor( bp, # base path
                                                         0, # min
                                                         matrixSize, # max
                                                         matrixScale, # scale
                                                         NLinearInterpolatorFactory() ) # interpolation
             
        bp = home + "/array/array_%02d.tif"
        make_sure_path_exists( bp )
        arrayTracker = CorrelationArrayTrackerVisitor( bp, # base path
                                                       FloorInterpolatorFactory(), # interpolation
                                                       imgSource.getStack().getSize(), # number of data points
                                                       correlationRange ) # range for pairwise correlations
             
        bp = home + "/render/render_%02d.tif"
        make_sure_path_exists( bp )
        hyperSlices = ArrayList()
        # hyperSlice = Views.hyperSlice( ImagePlusImgs.from( imgSource ), 1,  345 )
        # ImageJFunctions.show( hyperSlice )
             
        renderTracker = ApplyTransformToImagesAndAverageVisitor( bp, # base path
                                                                 FloorInterpolatorFactory(), # interpolation
                                                                 scale,
                                                                 0,
                                                                 0,
                                                                 imgSource.getWidth(),
                                                                 nImages)
        for i in xrange(-2, 3, 1):
            renderTracker.addImage( Views.hyperSlice( ImagePlusImgs.from( imgSource ), 1,  5 + i ) )                                                 
             
        bp = home + "/fit_tracker/fitTracker_%d.csv"
        make_sure_path_exists( bp )
        separator = ','
        fitTracker = CorrelationFitTrackerVisitor( bp, # base path
                                                   correlationRange, # range
                                                   separator ) # csv separator
             
        bp = home + "/fit_coordinates/fitCoordinates_%d.csv"
        make_sure_path_exists( bp )
        coordinateTracker = ActualCoordinatesTrackerVisitor( bp,
                                                             separator )
                                                             
        bp = home + "/multipliers/multipliers_%d.csv"
        make_sure_path_exists( bp )
        multiplierTracker = MultipliersTrackerVisitor( bp,
                                                       separator )
             
        bp = home + "/weights/weights_%d.csv"
        make_sure_path_exists( bp )
        weightsTracker = WeightsTrackerVisitor( bp,
                                                separator )                                                                                                          
             
        matrixTracker.addVisitor( arrayTracker )
        matrixTracker.addVisitor( renderTracker )
        matrixTracker.addVisitor( fitTracker )
        matrixTracker.addVisitor( coordinateTracker )
        matrixTracker.addVisitor( multiplierTracker )
        matrixTracker.addVisitor( weightsTracker )                                         
             
        # if you want to specify values for options, do:
        # options.multiplierGenerationRegularizerWeight = <value>
        # or equivalent
        result = inference.estimateZCoordinates( 0, 0, startingCoordinates, matrixTracker, options )
             
             
        # array = jarray.zeros( result.dimension(0), 'd' )
        # cursor = result.cursor()
        # for i in xrange(result.dimension(0) ):
        #     array[i] = scale * cursor.next().get()
             
        # lutTransform = SingleDimensionLUTRealTransform( array, 3, 3, 2 )
             
        # resultImage = ImagePlusImgs.unsignedBytes( imgSource.getWidth(), imgSource.getHeight(), int(scale) * stack.getSize() )
        # interpolated = Views.interpolate( Views.extendValue( ImagePlusImgs.from(imgSource), DoubleType( Double.NaN ) ), FloorInterpolatorFactory() )
        # transformed =  Views.interval( RealViews.transform( interpolated, lutTransform ), resultImage )
             
        # print type(transformed)
        # print type(resultImage)
        # CopyFromIntervalToInterval.copy( transformed, resultImage )
             
             
        # ImageJFunctions.show( resultImage )
             
             
             
             
        # t4 = time.time()
        # print t4 - t0
        # print
        # for r in result:
        #     print r
             
             
             
        # metaMap = co.getMetaMap()
        # fitMap = co.getFitMap()
        # for entryset in fitMap.entrySet():
        #     show = ImgLib2Display.copyToImagePlus(entryset.getValue())
        #     show.show()
        #     print entryset
             
 
