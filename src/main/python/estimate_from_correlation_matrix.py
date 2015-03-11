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

    r = 105 # range in z for pairwise similarity evaluation
    
    root       = '' # (intermediate) results will be stored within this directory
    sourceFile = '' # correlation matrix (image file), use current image, if empty

    if sourceFile == '':
        imgSource = IJ.getImage()
    else:
        imgSource = ImagePlus( sourceFile )
    ImageConverter( imgSource ).convertToGray32()
    nImages = imgSource.getHeight()
    stat    = FloatStatistics( imgSource.getProcessor() )
    normalizeBy = stat.max
    imgSource.getProcessor().multiply( 1.0 / normalizeBy ) # make sure similarities are in interval [0,1]
    
    matrixSize = nImages 
    matrixScale = 2.0 # increase matrix resolution (in case it is rendered)
    serializeCorrelations = True # save correlations to file?
    deserializeCorrelations = not serializeCorrelations # read correlations from file?
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6 # only shift by this fraction in each iteration
    options.nIterations = 100 # run for this number of iterations
    options.nThreads = 1
    options.windowRange = 100
    options.shiftsSmoothingSigma = 4
    options.shiftsSmoothingRange = 0 # smoothing between adjacent shifts, set to 0 for no smoothing
    options.withRegularization = True
    options.minimumSectionThickness = 0.01 # minimum section thickness if no reordering
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 
    options.multiplierGenerationRegularizerWeight = 0.1 # how stronlgy should mulitpliers be regularized to 1.0?
    options.multiplierEstimationIterations = 10 # number of iterations in inner loop for multiplier estimation
    options.withReorder = True # allow reordering of section
    options.coordinateUpdateRegularizerWeight = 0.0 # how strongly should coordinates be regularized to initial coordinates?


    correlationRange = r                                                                              
    home = root.rstrip('/') + '/range=%d_%s'.rstrip('/')                                              
    home = home % ( correlationRange, str(datetime.datetime.now() ) )                                 
    make_sure_path_exists( home.rstrip('/') + '/' )                                                   
                                                                                                      
    options.comparisonRange = correlationRange                                                                  
                                                                                                      
                                                                                                      
    optionsFile = '%s/options' % home.rstrip('/')                                                     
    with open( optionsFile, 'w' ) as f:                                                               
        f.write( '%s\n' % options.toString() )                                                        
                                                                                                      
                                                                                                      
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
    # coordinateTracker.addVisitor( matrixTracker ) # uncomment if you want to render at each iteration
    coordinateTracker.addVisitor( multiplierTracker )
    coordinateTracker.addVisitor( weightsTracker )
    # coordinateTracker.addVisitor( floorTracker ) # uncomment if you want to render at each iteration
                                                                                                      
    matrix = ImageJFunctions.wrapFloat( imgSource )                                                   
    result = inference.estimateZCoordinates( matrix, startingCoordinates, coordinateTracker, options )
             

 
