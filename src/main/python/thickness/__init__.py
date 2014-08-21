import copy

from estimation import CorrelationsCreator


def generateDefaultOptionsAndOrderedKeys():

    DEFAULT_OPTIONS = {}
    DEFAULT_OPTIONS_KEYS = []

    def addToOptionsAndKeys( key, value ):
        assert type( value ) == tuple and len( value ) == 2
        DEFAULT_OPTIONS[ key ] = value
        DEFAULT_OPTIONS_KEYS.append( key )


    addToOptionsAndKeys( 'rootDir', ( 'Root directory', '' ) )
    addToOptionsAndKeys( 'dataDir', ( 'Data direcotry relative to root', 'data' ) )
    addToOptionsAndKeys( 'resultDir', ( 'Result directory relative to root', 'result' ) )
    addToOptionsAndKeys( 'correlationsRange', ( 'Correlation range', 10 ) )
    addToOptionsAndKeys( 'numberOfIterations', ( 'Number of iterations', 1 ) )
    addToOptionsAndKeys( 'numberOfThreads', ( 'Number of threads', 1 ) )
    addToOptionsAndKeys( 'step', ( 'Sample step in x and y', 1 ) )
    addToOptionsAndKeys( 'neighborRegularizerWeight', ( 'Weight for regularization on neighbors\' shifts', 0.1 ) )
    addToOptionsAndKeys( 'shiftProportion', ( 'Proportion by which coordinate is shifted towards estimate.', 0.5 ) )
    addToOptionsAndKeys( 'coordinateUpdateRegularizerWeight', ( 'Weight for regularizing towards integer coordinate grid.', 0.01 ) )
    addToOptionsAndKeys( 'interpolatorFactory', ( 'How to interpolate data after adjusting coordinates.', 'Floor' ) )

    return DEFAULT_OPTIONS, DEFAULT_OPTIONS_KEYS
    
        
