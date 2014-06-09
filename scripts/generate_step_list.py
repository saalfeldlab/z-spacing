#!/usr/bin/python2


import numpy as np


class NumberDrawer( object ):
    def __call__( self ):
        return 1


class ConstantStepDrawer( NumberDrawer ):
    def __init__( self, N ):
        self.N = N

    def __call__( self ):
        return self.N

    
class UniformNumberDrawer( NumberDrawer ):
    def __init__( self, upperLimit ):
        if upperLimit < 1:
            upperLimit = 1
        self.upperLimit = upperLimit
        self.lowerLimit = 1

    def __call__( self ):
        return np.random.random_integers( self.lowerLimit, self.upperLimit )


class NormalNumberDrawer( NumberDrawer ):
    def __init__( self, mean, stdev ):
        if ( mean < 1 ):
            mean = 1
        self.mean = mean
        self.stdev  = stdev

    def __call__( self ):
        return int( self.mean + self.stdev *  np.random.randn( 1 ) )
            

        
def createSteps( startFrame, endFrame, numberDrawer ):
    lower = startFrame
    upper = startFrame

    resultString = ''

    while ( upper < endFrame ):
        upper = min( max ( lower + numberDrawer(), lower + 1 ), endFrame )
        
        resultString += '%d,%d,%d\n' % ( lower, upper, upper - lower )
        
        lower = upper

    return resultString[:-1]


def createDrawer( drawer, parameters ):
    parameterDict = { line.split('=')[0] : float( line.split('=')[1] ) for line in parameters.split(',') }

    if drawer.lower() == "uniform":
        return UniformNumberDrawer( **parameterDict )
    elif drawer.lower() == "normal":
        return NormalNumberDrawer( **parameterDict )
    else:
        raise Exception( "No rule for drawer %s!" %  drawer )

if __name__ == "__main__":

    import argparse
    parser = argparse.ArgumentParser()

    parser.add_argument( '--start', '-s', required=True, type=int )
    parser.add_argument( '--end', '-e', required=True, type=int )
    parser.add_argument( '--drawer', '-d', default='uniform')
    parser.add_argument( '--drawer-parameters', '-p', default='upperLimit=5' )
    parser.add_argument( '--random-seed', '-r', type=int, default=100 )
    parser.add_argument( '--output', '-o', type=str, required=True )

    args = parser.parse_args()

    np.random.seed( args.random_seed )

    drawer = createDrawer( args.drawer, args.drawer_parameters )
    
    # print createSteps( args.start, args.end, drawer )

    stepsString = createSteps( args.start, args.end, drawer )

    with open(args.output, "w") as f:
        f.write( stepsString ) 
