from __future__ import with_statement # need this for with statement

from ij import ImagePlus
from ij import ImageStack
from ij import IJ
from ij.gui import GenericDialog
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

from net.imglib2 import FinalInterval
from net.imglib2.converter import RealDoubleConverter
from net.imglib2.converter.read import ConvertedRandomAccessibleInterval
from net.imglib2.img.array import ArrayImgs
from net.imglib2.img.display.imagej import ImageJFunctions
from net.imglib2.img import ImagePlusAdapter
from net.imglib2.interpolation.randomaccess import NLinearInterpolatorFactory
from net.imglib2.interpolation.randomaccess import NearestNeighborInterpolatorFactory
from net.imglib2.interpolation.randomaccess import FloorInterpolatorFactory
from net.imglib2.view import TransformView
from net.imglib2.view import Views
from net.imglib2.realtransform import RealViews
from net.imglib2.img.imageplus import ImagePlusImgs
from net.imglib2.type.numeric.real import DoubleType

from org.janelia.models import ScaleModel
from org.janelia.utility.tuple import ConstantPair
from org.janelia.utility import CopyFromIntervalToInterval
from org.janelia.utility.tuple import SerializableConstantPair
from org.janelia.utility.io import Serialization
from org.janelia.utility.transform import StripToMatrix
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

from threading import Thread

# import utility

def make_sure_path_exists(path):
    try:
        os.makedirs( os.path.dirname( path ) )
    except OSError, exception: # need comma here, 'as' does not work in jython
        if exception.errno != errno.EEXIST:
            raise

def run( matrix, startingCoordinates, coordinateTracker, options ):
    try:
        result = inference.estimateZCoordinates( matrix, startingCoordinates, coordinateTracker, options )
        print 'done processing sections % 6d to % 6d...' % ( lower, upper )
    except Exception, e:
        print 'failed processing sections % 6d to % 6d...' % ( lower, upper )
        

if __name__ == "__main__":



    t0 = time.time()
    print t0 - t0

    imgSource = IJ.getImage()

    root   = '/tier2/saalfeld/hanslovskyp/shan/thickness/'
    c      = imgSource.getWidth()/2
    height = imgSource.getHeight()

    dialog = GenericDialog( "Overlapping thickness estimation" )
    dialog.addStringField( "Root directory for storing results", root )
    dialog.addCheckbox( "Render matrix to image at each iteration.", False )
    dialog.addNumericField( "Start.", 0, 0 )
    dialog.addNumericField( "Stop.", height, 0 )
    dialog.addNumericField( "Interval size.", 1000, 0 )
    dialog.addNumericField( "Overlap.", imgSource.getWidth()/2, 0 )
    dialog.addNumericField( "Range.", c, 0 )

    dialog.showDialog()

    if dialog.wasCanceled():
        raise Exception( "dialog was canceled" )

    root     = dialog.getNextString()
    doRender = dialog.getNextBoolean()
    start    = dialog.getNextNumber()
    stop     = dialog.getNextNumber()
    interval = dialog.getNextNumber()
    overlap  = dialog.getNextNumber()
    c        = dialog.getNextNumber()
    step     = interval - overlap


    
    ImageConverter( imgSource ).convertToGray32()
    
    stat    = FloatStatistics( imgSource.getProcessor() )
    normalizeBy = stat.max
    imgSource.getProcessor().multiply( 1.0 / normalizeBy )
    nThreads = 1
    serializeCorrelations = True
    deserializeCorrelations = not serializeCorrelations
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6
    options.nIterations = 30
    options.nThreads = nThreads
    options.windowRange = 100
    options.shiftsSmoothingSigma = 4
    options.shiftsSmoothingRange = 0
    options.withRegularization = True
    options.minimumSectionThickness = 0.0001
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 # weights[ i ] = exp( -0.5*(multiplier[i] - 1.0)^2 / multiplierWeightSigma^2 )
    options.multiplierGenerationRegularizerWeight = 0.1
    options.multiplierEstimationIterations = 10
    options.withReorder = False
    options.coordinateUpdateRegularizerWeight = 0.0
    thickness_estimation_repo_dir = '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation'

    wholeStrip = ImageJFunctions.wrap( imgSource )

    timestamp  = str(datetime.datetime.now() )

    threads = []
    upper = start
    while upper < stop:
        correlationRange = int(c)
        lower            = max( 0, upper - overlap )
        upper            = lower + interval
        if upper + step >= stop:
            upper = min( stop, upper + step )
        
        home = root.rstrip('/') + '/range=%d_%s/lower=%d_upper=%d'
        home = home % ( correlationRange, timestamp, lower, upper )
        make_sure_path_exists( home.rstrip('/') + '/' )

        options.comparisonRange = int(c)

        subStrip = ConvertedRandomAccessibleInterval( Views.interval( wholeStrip, [long(0), long(lower)], [long(wholeStrip.dimension(0)-1), long(upper-1)] ),  RealDoubleConverter(), DoubleType() )


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
        
        nImages = subStrip.dimension(1)
        matrixSize = nImages
        matrixScale = 2.0
        startC = 1
        stopC  = nImages
        if False and stackMin != None:
            startC = stackMin
        if False and stackMax != None:
            stopC  = stackMax
        startingCoordinates = range( startC - 1, stopC )

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
        if doRender:
            coordinateTracker.addVisitor( matrixTracker )
            coordinateTracker.addVisitor( floorTracker )
        coordinateTracker.addVisitor( multiplierTracker )
        coordinateTracker.addVisitor( weightsTracker )

        
        # ImageJFunctions.show( subStrip )

        tf     = StripToMatrix( wholeStrip.dimension(0) / 2 )
        store  = ArrayImgs.doubles( subStrip.dimension( 0 ), subStrip.dimension( 1 ) )
        CopyFromIntervalToInterval.copyToRealType( subStrip, store )
        matrix = Views.interval( TransformView( Views.extendValue( store, DoubleType( Double.NaN ) ), tf ),
                                 FinalInterval( [subStrip.dimension(1), subStrip.dimension(1)] ) )
        # ImageJFunctions.show( matrix )

        

        # ImageJFunctions.show( subStrip )
        # ImageJFunctions.show( matrix )
        print "adding thread for lower=%d and upper=%d" % (lower, upper)
        t = Thread( target = run, args = ( matrix, startingCoordinates, coordinateTracker, options ) )
        t.start()
        threads.append( t )

    for t in threads:
        t.join()

        

 
