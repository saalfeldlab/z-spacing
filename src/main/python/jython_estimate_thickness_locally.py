from __future__ import with_statement # need this for with statement

from ij import ImagePlus
from ij import ImageStack
from ij import IJ
from ij.io import FileSaver
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
from org.janelia.correlations.pyramid import CorrelationsObjectPyramidFactory
from org.janelia.correlations.pyramid import InferFromCorrelationsObjectPyramid
from org.janelia.thickness.inference import InferFromCorrelationsObject
from org.janelia.thickness.inference import Options
from org.janelia.thickness.inference import MultiScaleEstimation
from org.janelia.thickness.inference.visitor import ActualCoordinatesTrackerVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImagesAndAverageVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImageVisitor
from org.janelia.thickness.inference.visitor import CorrelationArrayTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationFitTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationMatrixTrackerVisitor
from org.janelia.thickness.inference.visitor import LazyVisitor
from org.janelia.thickness.inference.visitor import MultipliersTrackerVisitor
from org.janelia.thickness.inference.visitor import PositionTrackerVisitor
from org.janelia.thickness.inference.visitor import WeightsTrackerVisitor
from org.janelia.thickness.inference.visitor.multiscale import CoordinateDifferenceMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import CoordinateDifferenceToGridMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import CoordinateMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import ListMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import OptionsMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import RadiiMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import RenderImageMultiScaleVisitor
from org.janelia.thickness.inference.visitor.multiscale import StepsMultiScaleVisitor
from org.janelia.thickness.lut import SingleDimensionLUTRealTransform
from org.janelia.thickness.lut import SingleDimensionLUTGrid
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


if __name__ == "__main__":

    # import optparse

    # parser = optparse.OptionParser()
    # parser.add_option('--config', '-c', default='/dev/null', help='Config file that stores parameters and filenames.')

    # options, args = parser.parse_args()


    t0 = time.time()
    print t0 - t0

    correlationRanges = range( 10, 1001, 222221 )
    nImages = 237
    # root = '/data/hanslovskyp/playground/pov-ray/constant_thickness=5/850-1149/scale/0.05/250x250+125+125'
    # root = '/data/hanslovskyp/export_from_nobackup/sub_stack_01/data/substacks/01/'
    # root = '/ssd/hanslovskyp/crack_from_john/substacks/03/'
    # root = '/ssd/hanslovskyp/forPhilipp/substacks/03/'
    # root = '/data/hanslovskyp/boergens/substacks/01/'
    # root = '/data/hanslovskyp/forPhilipp/substacks/03/'
    # root = '/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/'
    # root = '/data/hanslovskyp/playground/pov-ray/variable_thickness_subset2/2200-2799/scale/0.04/200x200+100+100/'
    # root = '/data/hanslovskyp/playground/pov-ray/constant_thickness=5/850-1149/scale/0.05/250x250+125+125/'
    # root = '/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/'
    # root = '/ssd/hanslovskyp/tweak_CutOn4-15-2013_ImagedOn1-27-2014/substacks/01/distorted/02/'
    # root = '/data/hanslovskyp/crack_from_john/substacks/03/'
    # root = '/data/hanslovskyp/davi_toy_set/'
    # root = '/data/hanslovskyp/davi_toy_set/substacks/remove/01/'
    # root = '/data/hanslovskyp/davi_toy_set/substacks/replace_by_average/01/'
    # root = '/data/hanslovskyp/davi_toy_set/substacks/shuffle/03/'
    # root = '/data/hanslovskyp/jain-nobackup/234/'
    root = '/data/hanslovskyp/jain-nobackup/234_data_downscaled/crop-150x150+75+175/'
    # root = '/data/hanslovskyp/jain-nobackup/234/substacks/crop-150x150+75+175/'
    IJ.run("Image Sequence...", "open=%s/data number=%d sort" % ( root.rstrip(), nImages ) );
    # imgSource = FolderOpener().open( '%s/data' % root.rstrip('/') )
    imgSource = IJ.getImage()
    # imgSource.show()
    conv = ImageConverter( imgSource )
    conv.convertToGray32()
    stackSource = imgSource.getStack()
    nThreads = 50
    scale = 1.0
    # stackMin, stackMax = ( None, 300 )
    # xyScale = 0.25 # fibsem (crack from john) ~> 0.25
    # xyScale = 0.1 # fibsem (crop from john) ~> 0.1? # boergens
    nImages = stackSource.getSize()
    xyScale = 1.0
    doXYScale = False
    matrixSize = nImages
    matrixScale = 1
    serializeCorrelations = False
    deserializeCorrelations = not serializeCorrelations
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6
    options.nIterations = 100
    options.nThreads = nThreads
    options.windowRange = nImages
    options.shiftsSmoothingSigma = 1.5
    options.shiftsSmoothingRange = 0
    options.withRegularization = True
    options.minimumSectionThickness = 0.00000001 # Double.NaN # 0.1
    options.withReorder = False
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 # weights[ i ] = exp( -0.5*(multiplier[i] - 1.0)^2 / multiplierWeightSigma^2 )
    options.multiplierGenerationRegularizerWeight = 0.1
    options.multiplierEstimationIterations = 10
    options.coordinateUpdateRegularizerWeight = 0.0
    thickness_estimation_repo_dir = '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation'

    options2 = options.clone()
    options2.shiftProportion = 0.5
    options2.nIterations = 10
    options2.shiftsSmoothingRange = 0
    options2.withRegularization = False
    # options2.minimumSectionThickness = 0.05 # Double.NaN
    options2.withReorder = False
    options2.multiplierGenerationRegularizerWeight = 0.1
    options2.multiplierEstimationIterations = 10
    options2.coordinateUpdateRegularizerWeight = 0.3

    if not doXYScale:
        xyScale = 1.0
    
   

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

    width  = img.getWidth()
    height =  img.getHeight()

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
        

             
             
        start = 1
        stop  = img.getStack().getSize()
        if False and stackMin != None:
            start = stackMin
        if False and stackMax != None:
            stop  = stackMax
        startingCoordinates = range( start - 1, stop )

        wrappedImage = ImagePlusAdapter.wrap( img )
        mse    = MultiScaleEstimation( wrappedImage )
        radii  = [ [ width, height ], [ 75, 75 ], [ 30, 30 ], [ 15, 15 ], [ 15, 15 ] ]#, [ 15, 15 ] ]
        steps  = [ [ width, height ], [ 75, 75 ], [ 30, 30 ], [ 15, 15 ], [ 3, 3 ] ]#, [ 1, 1 ] ]
        opt = [options]
        ratio = 0.8
        for idx in xrange(len(radii)-1):
            tmpOptions = options2.clone()
            tmpOptions.windowRange = int( opt[idx].windowRange * 0.8 )
            opt.append( tmpOptions )
        # opt    = [ options, options2, options2, options2, options2 ]#, options2 ]

        visitor = ListMultiScaleVisitor( ArrayList() )

        bp = home.rstrip('/') + '/transformed/%02d.tif'
        imageVisitor = RenderImageMultiScaleVisitor( wrappedImage, bp )
        visitor.addVisitor( imageVisitor )
    
        bp = home.rstrip('/') + '/coordinates/%02d.tif'
        coordinateVisitor = CoordinateMultiScaleVisitor( bp )
        visitor.addVisitor( coordinateVisitor )

        bp = home.rstrip('/') + '/coordinateDiff/%02d.tif'
        coordinateDiffVisitor = CoordinateDifferenceMultiScaleVisitor( bp )
        visitor.addVisitor( coordinateDiffVisitor )

        bp = home.rstrip('/') + '/coordinateDiffToGrid/%02d.tif'
        coordinateDiffToGridVisitor = CoordinateDifferenceToGridMultiScaleVisitor( bp )
        visitor.addVisitor( coordinateDiffToGridVisitor )

        bp = home.rstrip('/') + '/opts/%02d'
        optionsVisitor = OptionsMultiScaleVisitor( bp )
        visitor.addVisitor( optionsVisitor )

        bp = home.rstrip('/') + '/radii/%02d'
        radiiVisitor = RadiiMultiScaleVisitor( bp )
        visitor.addVisitor( radiiVisitor )

        bp = home.rstrip('/') + '/steps/%02d'
        stepsVisitor = StepsMultiScaleVisitor( bp )
        visitor.addVisitor( stepsVisitor )

        result = mse.estimateZCoordinates( startingCoordinates, c, radii, steps, visitor, opt )
        IJ.log("done")

        resultFileName = '%s/result.tif' % home.rstrip('/')
        imp = ImageJFunctions.wrap( result, 'result' )
        IJ.saveAsTiff(imp.duplicate(), resultFileName)

        relativeResult = result.copy()
        c = relativeResult.cursor()
        while c.hasNext():
            c.fwd()
            cur = c.get()
            val = cur.get()
            cur.set( val - c.getDoublePosition( 2 ) )

        relativeResultFileName = '%s/relativeResult.tif' % home.rstrip('/')
        imp = ImageJFunctions.wrap( relativeResult, 'relative result' )
        IJ.saveAsTiff(imp.duplicate(), relativeResultFileName)

        ratio = [ wrappedImage.dimension( 0 )*1.0/result.dimension( 0 ), wrappedImage.dimension( 1 )*1.0/result.dimension( 1 ) ]
        shift = [ 0.0, 0.0 ]
        lutField = SingleDimensionLUTGrid(3, 3, result, 2, ratio, shift )

        transformed = Views.interval( Views.raster( RealViews.transformReal( Views.interpolate( Views.extendBorder( wrappedImage ), NLinearInterpolatorFactory() ), lutField ) ), wrappedImage )
        imp = ImageJFunctions.wrap( transformed, 'transformed' )
        transformedFileName = '%s/transformed.tif' % home.rstrip('/')
        IJ.saveAsTiff( imp.duplicate(), transformedFileName )
        
        # result = inference.estimateZCoordinates( 0, 0, startingCoordinates, matrixTracker, options )

             
 
