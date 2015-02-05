# python
import time

from ij import IJ
from ij.plugin import ImageCalculator
from ij.process import FloatStatistics

from java.lang import Runtime

# em-thickness-estimation
from org.janelia.correlations import CrossCorrelations;

from net.imglib2.img.display.imagej import ImageJFunctions
from net.imglib2.type.numeric.real import DoubleType

def timeCreation( data, xy, r, radius, nThreads, t ):
    t0 = time.time()
    matrix = CrossCorrelations.toMatrix( data, xy, r, radius, nThreads, t )
    t1 = time.time()
    dt = t1 - t0

    return dt, t1, t0, matrix


def compare( m1, m2 ):
    imp1 = ImageJFunctions.wrap( m1, "" )
    imp2 = ImageJFunctions.wrap( m2, "" )
    ic   = ImageCalculator()
    res  = ic.run("Subtract create", imp1, imp2)
    stat = FloatStatistics( res.getProcessor() )
    return stat.max == 0.0 and stat.min == 0.0



imp       = IJ.getImage();
data      = ImageJFunctions.wrap( imp )
xy        = [ 0, 0 ]
r         = min( 50, imp.getStack().getSize() )
radius    = [ imp.getWidth(), imp.getHeight() ]
t         = DoubleType()
nThreads1 = 1
nThreads2 = Runtime.getRuntime().availableProcessors()

dt1, t11, t01, matrix1 = timeCreation( data, xy, r, radius, nThreads1, t )
dt2, t12, t02, matrix2 = timeCreation( data, xy, r, radius, nThreads2, t )

r1String = "runtime1 = %fs (%d threads)" % ( dt1, nThreads1 )
r2String = "runtime2 = %fs (%d threads)" % ( dt2, nThreads2 )
IJ.log( r1String )
IJ.log( r2String )
print r1String
print r2String

equal = compare( matrix1, matrix2 )
equalString = "Resulting matrices are the same? " + str(equal)
IJ.log( equalString )
print equalString

# ImageJFunctions.show( matrix1 )
# ImageJFunctions.show( matrix2 )
    