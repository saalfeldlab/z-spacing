# because of lame old python 2.5 we need to import with_statement
from __future__ import with_statement

# python built-in
import inspect
import os
import re
import sys

# java
from java import awt
from java.lang import Float

# mpicbg
from mpicbg.ij.integral import BlockPMCC
from mpicbg.models import TranslationModel1D

# imglib2
from net.imglib2.img import ImagePlusAdapter
from net.imglib2.img.display.imagej import ImageJFunctions
from net.imglib2.img.display.imagej import ImgLib2Display
from net.imglib2.img.imageplus import ImagePlusImgs
from net.imglib2.interpolation.randomaccess import NLinearInterpolatorFactory
from net.imglib2.interpolation.randomaccess import NearestNeighborInterpolatorFactory
from net.imglib2.interpolation.randomaccess import FloorInterpolatorFactory
from net.imglib2.realtransform import RealViews
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

# ImageJ
from ij import IJ
from ij import ImagePlus
from ij import WindowManager
from ij.gui import GenericDialog
from ij.plugin import FolderOpener
from ij.plugin import ImageCalculator
from ij.plugin import Slicer
from ij.plugin import StackCombiner
from ij.process import ImageConverter

# Thickness estimation - java
from org.janelia.correlations import CorrelationsObject
from org.janelia.models import ScaleModel
from org.janelia.thickness import EstimateThicknessLocally
from org.janelia.thickness import InferFromCorrelationsObject # deprecated, move convertToFloat to utility
from org.janelia.thickness.lut import SingleDimensionLUTRealTransform
from org.janelia.thickness.mediator import OpinionMediatorModel
from org.janelia.utility import CopyFromIntervalToInterval


# Thickness estimation - python ( add this file's directory to PYTHONPATH first )
scriptRootFolder = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
sys.path.append( scriptRootFolder )
import thickness
import utility


if __name__ == "__main__":

    interpolatorFactories = \
    {
        'floor' : lambda: FloorInterpolatorFactory(), 
        'nlinear' : lambda: NLinearInterpolatorFactory(),
        'nearestneighbor' : lambda: NearestNeighborInterpolatorFactory()
    }

    if True: # replace with check for headless mode later
        defaultOptions, keys = thickness.generateDefaultOptionsAndOrderedKeys()

        if True: # ask for getting options from dialog or config file later                          
            optionsDialog = GenericDialog( "Local thickness estimation options" )
            optionsDialog.addRadioButtonGroup( 'Load image from file path', ['yes', 'no'], 1, 2, 'no' ) # change to yes
                                                             
            for k in keys:                                   
                v = defaultOptions[k]                        
                if type( v[1] ) == str:                      
                    optionsDialog.addStringField( *v )       
                else:                                        
                    vPrime = v + (4, )                       
                    optionsDialog.addNumericField( *vPrime ) 
                                                             
            optionsDialog.showDialog()
            if optionsDialog.wasCanceled():
                raise Exception( 'Canceled selection!' )                       
                                                             
            optionsDictionary = {}                                     
            for k in keys:                                   
                v = defaultOptions[k]                        
                if type( v[1] ) == str:                      
                    optionsDictionary[k] = optionsDialog.getNextString()
                else:                                        
                    optionsDictionary[k] = optionsDialog.getNextNumber()
                                                             
            selection = optionsDialog.getRadioButtonGroups()[0].getSelectedCheckbox();

            if selection.getLabel() == 'yes':                
                sourceImageGenerator = lambda: FolderOpener().open( '%s/%s' % ( optionsDictionary[ 'rootDir' ].rstrip('/'), optionsDictionary[ 'dataDir' ].rstrip('/') ) )
            elif selection.getLabel() == 'no':               
                sourceImageGenerator = lambda: WindowManager.getCurrentImage()

            while not optionsDictionary['interpolatorFactory'].lower() in interpolatorFactories.keys():
                keys = interpolatorFactories.keys()
                ifDialog = GenericDialog( 'Please specify interpolation type correctly (was %s)' % optionsDictionary['interpolatorFactory'] )
                ifDialog.addRadioButtonGroup( 'Choose interpolation type', keys, len( keys ), 1, keys[0] )
                ifDialog.showDialog()
                optionsDictionary['interpolatorFactory'] = ifDialog.getRadioButtonGroups()[0].getSelectedCheckbox().getLabel()
            

        else: # read options from config file
            raise Exception( "Not implemented yet!" )

    else: # headless mode
        raise Exception( "Not implemented yet!" )

    sourceImage = sourceImageGenerator()
    if sourceImage == None:
        raise Exception( "Image is empty!" )
    sourceStack = sourceImage.getStack()
    ImageConverter( sourceImage ).convertToGray32()
    
    options = EstimateThicknessLocally.Options.generateDefaultOptions()
    
    options.nIterations                       = int( optionsDictionary['numberOfIterations'] )
    options.nThreads                          = int( optionsDictionary['numberOfThreads'] )
    options.neighborRegularizerWeight         = float( optionsDictionary['neighborRegularizerWeight'] )
    options.shiftProportion                   = float( optionsDictionary['shiftProportion'] )
    options.coordinateUpdateRegularizerWeight = float( optionsDictionary['coordinateUpdateRegularizerWeight'] )

    radius            = [ int( optionsDictionary['crossCorrelationBlockRadius'] ) ] * 2
    step              = int( optionsDictionary['step'] )
    correlationsRange = int( optionsDictionary['correlationsRange'] )

    # do scaling?
    image = sourceImage
    
    creator = thickness.CorrelationsCreator( image, radius )
    creator.correlateAllWithinRange( correlationsRange )

    co = CorrelationsObject( )
    for i in xrange( 1, image.getStack().getSize() + 1 ):
        stackRange, interval = creator.toStackRange( i, correlationsRange )
        meta = CorrelationsObject.Meta()
        meta.zPosition = i - 1
        meta.zCoordinateMin = interval[0] - 1
        meta.zCoordinateMax = interval[1] # exclusive

        adapter = ImagePlusAdapter.wrap( ImagePlus( 'correlations_at_%d' % i, stackRange ) )
        co.addCorrelationImage( meta.zPosition, adapter, meta )

    inference = EstimateThicknessLocally( TranslationModel1D(),
                                          ScaleModel(),
                                          OpinionMediatorModel( TranslationModel1D() ),
                                          co )
    result     = inference.estimate( 0, 0, image.getWidth(), image.getHeight(), step, step, options )
    # do smoothing?
    resultDir  = re.sub( '/+', '/', '%s/%s' % ( optionsDictionary[ 'rootDir' ], optionsDictionary[ 'resultDir'] ) ).rstrip( '/' )
    resultDir  = utility.filesystem.create_with_counter_if_existing( resultDir ).rstrip( '/' )
    metaPath   = resultDir + '/meta'
    metaString = '\n'.join( '%s\t%s' % ( k, str( v ) ) for k, v in optionsDictionary.iteritems() )

    shifts              = ImgLib2Display.copyToImagePlus( InferFromCorrelationsObject.convertToFloat( result ) ) # depcreated, move convertToFloat to utility
    relativeShifts      = shifts.duplicate()
    relativeShiftsStack = relativeShifts.getStack()
    for i in xrange( relativeShiftsStack.getSize() ):
        ip = relativeShiftsStack.getProcessor( i + 1 )
        ip.add( -i )

    shiftsPath         = resultDir + '/shifts.tif'    
    relativeShiftsPath = resultDir + '/relativeShifts.tif'
    resultImagePath    = resultDir + '/result.tif'    
    combinedPath       = resultDir + '/combined.tif'  
    reslicedPath       = resultDir + '/reslicedCombined.tif'
    differencePath     = resultDir + '/difference.tif'

    interpolatorFactory = interpolatorFactories[ optionsDictionary['interpolatorFactory'].lower() ]()

    tf = InferFromCorrelationsObject.convertToTransformField2D( result, step, step, image.getWidth(), image.getHeight() ) # depcreated, move convertToTransformField2D to utility to different class
    interpolated = Views.interpolate( Views.extendValue( ImagePlusImgs.from( sourceImage), FloatType( Float.NaN ) ), interpolatorFactory )

    resultImage = ImagePlusImgs.floats( sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getStack().getSize() )
    transformed = Views.interval( RealViews.transform( interpolated, tf ), resultImage )

    CopyFromIntervalToInterval.copyToRealType( transformed, resultImage )

    resultImagePlus = ImageJFunctions.wrap( resultImage, '' ).duplicate()

    with open( metaPath, 'w' ) as f:
        f.write( metaString )

    IJ.save( shifts, shiftsPath )
    IJ.save( relativeShifts, relativeShiftsPath )
    IJ.save( resultImagePlus, resultImagePath )

    combined           = ImagePlus( 'combined', StackCombiner().combineHorizontally( sourceImage.duplicate().getStack(), resultImagePlus.duplicate().getStack() ) )
    reslicedSource     = Slicer().reslice( sourceImage )
    reslicedResult     = Slicer().reslice( resultImagePlus )                                                                                         
    combinedVertically = ImagePlus( 'combinedVertically', StackCombiner().combineVertically( reslicedSource.getStack(), reslicedResult.getStack() ) )

    IJ.save( combined, combinedPath )
    IJ.save( combinedVertically, reslicedPath )

    if True: # check for not headless!
        combined.show()

    ic = ImageCalculator()
    difference = ic.run( "Subtract create 32-bit stack", resultImagePlus, sourceImage )
    difference.hide()
    IJ.save( difference, differencePath )
 

    

    

        
    
