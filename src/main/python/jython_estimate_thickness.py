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

    t0 = time.time()
    print t0 - t0

    # correlationRanges = range( 10, 1001, 222221 )
    correlationRange = 10
    nImages = 63
    root = '/data/hanslovskyp/davi_toy_set/substacks/shuffle/03/'
    IJ.run("Image Sequence...", "open=%s/data number=%d sort" % ( root.rstrip(), nImages ) );
    imgSource = IJ.getImage()
    nImages = imgSource.getStack().getSize();
    # imgSource.show()
    conv = ImageConverter( imgSource )
    conv.convertToGray32()
    stackSource = imgSource.getStack()
    nThreads = 1
    scale = 5.0
    stackMin, stackMax = ( None, None )
    # xyScale = 0.25 # fibsem (crack from john) ~> 0.25
    # xyScale = 0.1 # fibsem (crop from john) ~> 0.1? # boergens
    xyScale = 0.1
    doXYScale = True
    matrixSize = nImages
    matrixScale = 10.0
    serializeCorrelations = True
    deserializeCorrelations = not serializeCorrelations
    options = Options.generateDefaultOptions()
    options.shiftProportion = 0.6
    options.nIterations = 100
    options.nThreads = nThreads
    options.windowRange = 100
    options.shiftsSmoothingSigma = 1.5
    options.shiftsSmoothingRange = 0
    options.withRegularization = True
    options.minimumSectionThickness = Double.NaN # 0.1
    options.multiplierRegularizerDecaySpeed = 50
    options.multiplierWeightsSigma = 0.04 # weights[ i ] = exp( -0.5*(multiplier[i] - 1.0)^2 / multiplierWeightSigma^2 )
    options.multiplierGenerationRegularizerWeight = 0.1
    options.coordinateUpdateRegularizerWeight = 0.01
    options.neighborRegularizerWeight = 0.05
    options.multiplierEstimationIterations = 10
    options.withReorder = True
    thickness_estimation_repo_dir = '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation'

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

    homeScale = root.rstrip('/') + '/xyScale=%f' % xyScale                                      
    home = homeScale.rstrip('/') + '/range=%d_%s'.rstrip('/')                                   
    home = home % ( correlationRange, str(datetime.datetime.now() ) )                           
    make_sure_path_exists( home.rstrip('/') + '/' )                                             
                                                                                                
    options.comparisonRange = correlationRange
                                                                                                
    serializationString = '%s/correlations_range=%d.tif' % ( homeScale.rstrip(), correlationRange )
                                                                                                
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
                                                                                                
    startingCoordinates = []                                                                    
                                                                                                
    start = 1                                                                                   
    stop  = img.getStack().getSize()                                                            
    if stackMin != None:                                                              
        start = stackMin                                                                        
    if stackMax != None:                                                              
        stop  = stackMax                                                                        
    startingCoordinates = range( start - 1, stop )                                              
                                                                                                
    t0Prime = time.time()                                                                       
    if deserializeCorrelations:
        matrix = ImagePlusAdatper.wrapFloat( ImagePlus( serialiationString ) )                                                 
    else:
        matrix = CrossCorrelations.toMatrix(
            ImagePlusAdapter.wrap( img ), # wrap input to RandomAccessibleInterval
            [ Long(0), Long(0) ], # coordinates (take any, as we correlate complete image)
            correlationRange, # range for pairwise similarities
            [ img.getWidth(), img.getHeight() ], # radii of correlation window (complete image)
            DoubleType() # dummy object necessary for call to generic function
             )
        FileSaver( ImageJFunctions.wrap( matrix, "wrapped matrix for writing" ) ).saveAsTiff( serializationString )
                                                                                                
    t3 = time.time()                                                                            
    print t3 - t0Prime                                                                          
                                                                                                
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
                                                                                                
                                                                                                
                                                                                                
    bp = home + "/render/render_%02d.tif"                                                       
    make_sure_path_exists( bp )                                                                 
    hyperSlices = ArrayList()                                                                   
                                                                                                
    renderTracker = ApplyTransformToImagesAndAverageVisitor( bp, # base path                    
                                                             FloorInterpolatorFactory(), # interpolation
                                                             scale,                             
                                                             0,                                 
                                                             0,                                 
                                                             imgSource.getWidth(),              
                                                             nImages)                           
    for i in xrange(-2, 3, 1):                                                                  
        renderTracker.addImage( Views.hyperSlice( ImagePlusImgs.from( imgSource ), 1,  30 + i ) )                                                 
                                                                                                
    renderTracker.average()                                                                     
                                                                                                
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
    matrixTracker.addVisitor( floorTracker )
                                                                                                
    # if you want to specify values for options, do:                                            
    # options.multiplierGenerationRegularizerWeight = <value>                                   
    # or equivalent
    IJ.log( str( len( startingCoordinates ) ) + " MIZZZZGE" )                                                  
    result = inference.estimateZCoordinates( matrix, startingCoordinates, matrixTracker, options )
 
