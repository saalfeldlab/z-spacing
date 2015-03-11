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

from mpicbg.models import TranslationModel1D
from mpicbg.ij.util import Filter

from net.imglib2.img.display.imagej import ImageJFunctions
from net.imglib2.img import ImagePlusAdapter
from net.imglib2.interpolation.randomaccess import NLinearInterpolatorFactory
from net.imglib2.interpolation.randomaccess import NearestNeighborInterpolatorFactory
from net.imglib2.interpolation.randomaccess import FloorInterpolatorFactory
from net.imglib2.view import Views
from net.imglib2.img.imageplus import ImagePlusImgs
from net.imglib2.type.numeric.real import DoubleType

from org.janelia.correlations import CrossCorrelations

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

    correlationRange = 10
    nImages = 100
    root    = '' # specify root for results
    dataDir = '' # specify data dir, if empty, use current stack
    if not dataDir == '':
        IJ.run("Image Sequence...", "open=%s sort" % ( dataDir ) );
    imgSource = IJ.getImage()
    nImages = imgSource.getStack().getSize();
    # imgSource.show()
    conv = ImageConverter( imgSource )
    conv.convertToGray32()
    stackSource = imgSource.getStack()
    nThreads = 1 # specify threads if necessary
    scale = 5.0
    stackMin, stackMax = ( None, None )
    xyScale = 0.1 # by what factor scale x and y, if doXYScale is True
    doXYScale = False 
    matrixSize = nImages
    matrixScale = 10.0 # by what factor increase matrix resolution if it is rendered at each iteration
    serializeCorrelations = True # save correlations to file?
    deserializeCorrelations = not serializeCorrelations # read correlations from file?
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6 # only shift by this fraction in each iteration
    options.nIterations = 200 # run for this number of iterations
    options.nThreads = nThreads
    options.windowRange = 100
    options.shiftsSmoothingSigma = 1.5
    options.shiftsSmoothingRange = 0 # smoothing between adjacent shifts, set to 0 for no smoothing
    options.withRegularization = True
    options.minimumSectionThickness = 0.1 # minimum section thickness if no reordering
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 
    options.multiplierGenerationRegularizerWeight = 0.1 # how stronlgy should mulitpliers be regularized to 1.0?
    options.coordinateUpdateRegularizerWeight = 0.01 # how strongly should coordinates be regularized to initial coordinates?
    options.neighborRegularizerWeight = 0.05
    options.multiplierEstimationIterations = 10 # number of iterations in inner loop for multiplier estimation
    options.withReorder = True # how strongly should coordinates be regularized to initial coordinates?

    if not doXYScale:
        xyScale = 1.0
    
   

    img = imgSource.clone()
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

    homeScale = root.rstrip('/') + '/xyScale=%f' % xyScale                                      
    home = homeScale.rstrip('/') + '/range=%d_%s'.rstrip('/')                                   
    home = home % ( correlationRange, str(datetime.datetime.now() ) )                           
    make_sure_path_exists( home.rstrip('/') + '/' )                                             
                                                                                                
    options.comparisonRange = correlationRange
                                                                                                
    serializationString = '%s/correlations_range=%d.tif' % ( homeScale.rstrip(), correlationRange )
                                                                                                
    optionsFile = '%s/options' % home.rstrip('/')                                               
    with open( optionsFile, 'w' ) as f:                                                         
        f.write( '%s\n' % options.toString() )                                                  
                                                                                                
                                                                                                
    startingCoordinates = []                                                                    
                                                                                                
    start = 1                                                                                   
    stop  = img.getStack().getSize()                                                            
    if stackMin != None:                                                              
        start = stackMin                                                                        
    if stackMax != None:                                                              
        stop  = stackMax                                                                        
    startingCoordinates = range( start - 1, stop )                                              
                                                                                                
    if deserializeCorrelations:
        matrix = ImagePlusAdatper.wrapFloat( ImagePlus( serialiationString ) )                                                 
    else:
        matrix = CrossCorrelations.toMatrix(
            ImagePlusAdapter.wrap( img ), # wrap input to RandomAccessibleInterval
            [ Long(0), Long(0) ], # coordinates (take any, as we correlate complete image)
            correlationRange, # range for pairwise similarities
            [ img.getWidth(), img.getHeight() ], # radii of correlation window (complete image),
            nThreads, # number of threads
            DoubleType() # dummy object necessary for call to generic function
             )
        FileSaver( ImageJFunctions.wrap( matrix, "wrapped matrix for writing" ) ).saveAsTiff( serializationString )
                                                                                                
                                                                                                
    inference = InferFromMatrix( 
                                 TranslationModel1D(),
                                 OpinionMediatorModel( TranslationModel1D() )
                                )
                                                                                                
                                                                                                
    bp = home + "/matrix_floor/matrixFloor_%02d.tif"                                            
    make_sure_path_exists( bp )                                                                 
    floorTracker = CorrelationMatrixTrackerVisitor( bp, # base path                             
                                                     0, # min                                   
                                                     matrixSize, # max                          
                                                     matrixScale, # scale                       
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

                                             
    coordinateTracker.addVisitor( arrayTracker )
    coordinateTracker.addVisitor( fitTracker )
    # coordinateTracker.addVisitor( matrixTracker ) # uncomment if matrix should be rendered at each iteration
    coordinateTracker.addVisitor( multiplierTracker )
    coordinateTracker.addVisitor( weightsTracker )
    # coordinateTracker.addVisitor( floorTracker ) # uncomment if matrix should be rendered at each iteration
                                                                                                
    # if you want to specify values for options, do:                                            
    # options.multiplierGenerationRegularizerWeight = <value>                                   
    # or equivalent
    result = inference.estimateZCoordinates( matrix, startingCoordinates, coordinateTracker, options )
 
