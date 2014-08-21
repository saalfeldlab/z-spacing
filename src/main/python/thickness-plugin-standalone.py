# because of lame old python 2.5 we need to import with_statement
from __future__ import with_statement

# python built-in
import inspect
import os
import sys

# java
from java import awt

# ImageJ
from ij import WindowManager
from ij.gui import GenericDialog
from ij.plugin import FolderOpener

# Thickness estimation ( add this file's directory to PYTHONPATH first )
scriptRootFolder = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
sys.path.append( scriptRootFolder )
import thickness
import utility


if __name__ == "__main__":

    if True: # replace with check for headless mode later
        defaultOptions, keys = thickness.generateDefaultOptionsAndOrderedKeys()
                                  
        optionsDialog = GenericDialog( "Local thickness estimation options" )
        optionsDialog.addRadioButtonGroup( 'Load image from file path', ['yes', 'no'], 1, 2, 'yes' )
                                  
        for k in keys:            
            v = defaultOptions[k] 
            if type( v[1] ) == str:
                optionsDialog.addStringField( *v )
            else:                 
                vPrime = v + (4, )
                optionsDialog.addNumericField( *vPrime )
                                  
        optionsDialog.showDialog()

        options = {}
        for k in keys:
            v = defaultOptions[k]
            if type( v[1] ) == str:
                options[k] = optionsDialog.getNextString()
            else:
                options[k] = optionsDialog.getNextNumber()

        selection = optionsDialog.getRadioButtonGroups()[0].getSelectedCheckbox();

        if selection.getLabel() == 'yes':
            sourceImage = FolderOpener().open( '%s/%s' % ( options[ 'rootDir' ].rstrip('/'), options[ 'dataDir' ].rstrip('/') ) )
        elif selection.getLabel() == 'no':
            sourceImage = WindowManager.getCurrentImage()

    else: # headless mode
        pass

    print options
    print sourceImage
