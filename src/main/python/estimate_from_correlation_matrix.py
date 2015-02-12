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
from net.imglib2.interpolation.randomaccess import NLinearInterpolatorFactory
from net.imglib2.interpolation.randomaccess import NearestNeighborInterpolatorFactory
from net.imglib2.interpolation.randomaccess import FloorInterpolatorFactory
from net.imglib2.view import Views
from net.imglib2.realtransform import RealViews
from net.imglib2.img.imageplus import ImagePlusImgs
from net.imglib2.type.numeric.real import DoubleType

from org.janelia.models import ScaleModel
from org.janelia.utility.tuple import ConstantPair
from org.janelia.utility import CopyFromIntervalToInterval
from org.janelia.utility.tuple import SerializableConstantPair
from org.janelia.utility.io import Serialization
from org.janelia.utility.sampler import SparseXYSampler
from org.janelia.thickness.inference import InferFromMatrix
from org.janelia.thickness.inference import Options
from org.janelia.thickness.inference.visitor import ActualCoordinatesTrackerVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImagesAndAverageVisitor
from org.janelia.thickness.inference.visitor import ApplyTransformToImageVisitor
from org.janelia.thickness.inference.visitor import CorrelationArrayTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationFitTrackerVisitor
from org.janelia.thickness.inference.visitor import CorrelationMatrixTrackerVisitor
from org.janelia.thickness.inference.visitor import MultipliersTrackerVisitor
from org.janelia.thickness.inference.visitor import WeightsTrackerVisitor
from org.janelia.thickness.lut import SingleDimensionLUTRealTransform
from org.janelia.thickness.mediator import OpinionMediatorModel
from org.janelia.thickness.mediator import OpinionMediatorWeightedAverage

from ij.process import FloatStatistics


import datetime
import errno
import inspect
import jarray
import math
import os
import time
import shutil
import sys

# import utility

def make_sure_path_exists(path):
    try:
        os.makedirs( os.path.dirname( path ) )
    except OSError, exception: # need comma here, 'as' does not work in jython
        if exception.errno != errno.EEXIST:
            raise

if __name__ == "__main__":



    t0 = time.time()
    print t0 - t0

    r = 105
    correlationRanges = range( r, r + 1 )
    # root = '/data/hanslovskyp/forKhaled'
    # root = '/data/hanslovskyp/khaled_2014_10_22/'
    # root = '/data/hanslovskyp/khaled_2014_10_24/'
    # sourceFile = '/data/hanslovskyp/forKhaled/corr_matrix.tif' # output dir
    # sourceFile = '/data/hanslovskyp/khaled_2014_10_22/cross_corr_mx.tif'
    # sourceFile = '/data/hanslovskyp/khaled_2014_10_24/cross_corr_mx_x16.tif'
    # root       = '/groups/saalfeld/home/hanslovskyp/local/tmp/some_matrix/'
    # sourceFile = '/groups/saalfeld/home/hanslovskyp/local/tmp/some_matrix/mat.tif'
    root       = '/data/hanslovskyp/boergens/4/'
    sourceFile = '/data/hanslovskyp/boergens/4/ncc-matrix.tif'
    root       = '/data/hanslovskyp/boergens/2/'
    sourceFile = '/data/hanslovskyp/boergens/2/inlier ratio matrix.tif'
    root       = '/data/hanslovskyp/shan/scale/range=300/'
    sourceFile = '/data/hanslovskyp/shan/scale/range=300/inlier ratio matrix.tif'
    root       = '/tier2/saalfeld/hanslovskyp/spark-tests/'
    sourceFile = '/tier2/saalfeld/hanslovskyp/spark-tests/spark-matrix-11999.tif'
    
    imgSource = ImagePlus( sourceFile )
    ImageConverter( imgSource ).convertToGray32()
    nImages = imgSource.getHeight()
    stat    = FloatStatistics( imgSource.getProcessor() )
    normalizeBy = stat.max
    imgSource.getProcessor().multiply( 1.0 / normalizeBy )
    nThreads = 1
    matrixSize = nImages
    matrixScale = 2.0
    serializeCorrelations = True
    deserializeCorrelations = not serializeCorrelations
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6
    options.nIterations = 100
    options.nThreads = nThreads
    options.windowRange = 100
    options.shiftsSmoothingSigma = 4
    options.shiftsSmoothingRange = 0
    options.withRegularization = True
    options.minimumSectionThickness = Double.NaN # 0.01
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 # weights[ i ] = exp( -0.5*(multiplier[i] - 1.0)^2 / multiplierWeightSigma^2 )
    options.multiplierGenerationRegularizerWeight = 0.1
    options.multiplierEstimationIterations = 10
    options.withReorder = True
    options.coordinateUpdateRegularizerWeight = 0.0
    thickness_estimation_repo_dir = '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation'


    # img.show()
    for c in correlationRanges:    
        correlationRange = c
        home = root.rstrip('/') + '/range=%d_%s'.rstrip('/')
        home = home % ( correlationRange, str(datetime.datetime.now() ) )
        make_sure_path_exists( home.rstrip('/') + '/' )

        options.comparisonRange = c


        gitCommitInfoFile = '%s/commitHash' % home.rstrip('/')
        #with open( gitCommitInfoFile, 'w' ) as f:
        #    f.write( '%s\n' % utility.gitcommit.getCommit( thickness_estimation_repo_dir ) )

        gitDiffFile = '%s/gitDiff' % home.rstrip('/')
        #with open( gitDiffFile, 'w' ) as f:
        #    f.write( '%s\n' % utility.gitcommit.getDiff( thickness_estimation_repo_dir ) )


        optionsFile = '%s/options' % home.rstrip('/')
        with open( optionsFile, 'w' ) as f:
            f.write( '%s\n' % options.toString() )
        

        this_file_name = os.path.realpath( inspect.getfile( lambda : None ) ) # inspect.getfile requires method, class, ... as input and returns the file in which input was defined
        shutil.copyfile( this_file_name, '%s/%s' % ( home.rstrip('/'), this_file_name.split('/')[-1] ) )
        
             
        start = 1
        stop  = nImages
        if False and stackMin != None:
            start = stackMin
        if False and stackMax != None:
            stop  = stackMax
        startingCoordinates = range( start - 1, stop )

        inference = InferFromMatrix(
            TranslationModel1D(),
            OpinionMediatorWeightedAverage()#OpinionMediatorModel( TranslationModel1D() )
            )

        bp = home + "/matrix_nlinear/matrixNLinear_%02d.tif"
        make_sure_path_exists( bp )
        matrixTracker = CorrelationMatrixTrackerVisitor( bp, # base path
                                                         0, # min
                                                         matrixSize, # max
                                                         matrixScale, # scale
                                                         NLinearInterpolatorFactory() ) # interpolation

        bp = home + "/matrix_floor/matrixFloor_%02d.tif"
        make_sure_path_exists( bp )
        floorTracker = CorrelationMatrixTrackerVisitor( bp, # base path
                                                        0, # min
                                                        matrixSize, # max
                                                        matrixScale, # scale
                                                        FloorInterpolatorFactory() ) # interpolation
                                                
             
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

        coordinateTracker.addVisitor( fitTracker )
        # coordinateTracker.addVisitor( matrixTracker )
        coordinateTracker.addVisitor( multiplierTracker )
        coordinateTracker.addVisitor( weightsTracker )      
        # coordinateTracker.addVisitor( floorTracker )
             
        matrix = ImageJFunctions.wrapFloat( imgSource )
        result = inference.estimateZCoordinates( matrix, startingCoordinates, coordinateTracker, options )
             

 
