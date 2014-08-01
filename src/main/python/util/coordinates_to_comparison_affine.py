import csv
import glob
import ntpath
import os
import errno


from java.util import ArrayList

from mpicbg.models import AffineModel1D
from mpicbg.models import Point
from mpicbg.models import PointMatch


def make_sure_path_exists(path):
    try:
        os.makedirs( path )
        
    except OSError, exception: # need comma here, 'as' does not work in jython
        if exception.errno != errno.EEXIST:
            raise
            

def csvToArray( filename, delimiter, column ):
    res = []
    f = open(filename, 'rb' )
    reader = csv.reader( f, delimiter=delimiter )
    for row in reader:
        res.append( float( row[ coordinateColumn] ) )
    f.close()
    return res


def arrayToCsv( arr, filename, delimiter ):
    f = open( filename, 'wb' )
    writer = csv.writer( f, delimiter=delimiter )
    for a in arr:
        writer.writerow( [ a ] )
    f.close()


def fitAffine( coordinates, reference ):
    assert len( coordinates ) == len( reference ), "size inconsistency for coordinates and reference"
    res = []
    m = AffineModel1D()
    matches = ArrayList()
    for c, r in zip( coordinates, reference ):
        matches.add( PointMatch( Point( [ float(c) ] ), Point( [ float(r) ] ) ) )

    m.fit( matches )

    for c in coordinates:
        res.append( m.apply( [ float(c) ] )[0] )
    
    return res








if __name__ == "__main__":
    cRange = 5
    srcPattern = '/ssd/hanslovskyp/playground/pov-ray/variable_thickness_subset1/750-1049/scale/0.04/200x200+100+100/range=%d/fit_coordinates/*csv' % cRange
    filenames  = sorted( glob.glob( srcPattern ) )
    targetDir  = '/ssd/hanslovskyp/playground/pov-ray/variable_thickness_subset1/750-1049/scale/0.04/200x200+100+100/range=%d/fit_coordinates_transformed' % cRange
    make_sure_path_exists( targetDir +'/' )

    referenceFileName = '/ssd/hanslovskyp/playground/pov-ray/variable_thickness_subset1/750-1049/coordinates_zero_based'
    
    coordinateColumn = 1
    referenceColumn  = 1
    delimiter        = ','

    reference = csvToArray( referenceFileName, delimiter, referenceColumn )

    for fn in filenames:
    
        coordinates = csvToArray( fn, delimiter, coordinateColumn )
        transformed = fitAffine( coordinates, reference )
        fnBase = ntpath.basename( fn )
        arrayToCsv( transformed, '%s/%s' % ( targetDir.rstrip('/'), fnBase ), delimiter )
        # print '%s/%s' % ( targetDir.rstrip('/'), fnBase )
        
        
    



    