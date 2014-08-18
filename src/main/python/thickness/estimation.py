import errno
import jarray
import math
import os
import time


class CorrelationsCreator(object):
    """Calculate pairwise cross-correlations from an ImagePlus.

:param imagePlus: input image stack of type ImagePlus from which correlations are calculated
:param radius: x and y radius for cross-correlation window
:param offset: offset by which images are shifted for cross-correlation calculation
"""
    def __init__(self, imagePlus, radius=[8, 8], offset=[0,0]):
        self.correlations = {}
        self.radius = radius
        self.imagePlus = imagePlus
        self.offset = [0,0]

    def correlate(self, index1, index2):
        """Correlate slices of member imagePlus at given indices
:param index1: index of first slice
:param index2: index of second slice

:returns: cross correlation image for index1, index2
:rtype: FloatProcessor
"""        
        if (index1, index2) in self.correlations.keys():
            pass 
        elif (index2, index1) in self.correlations.keys():
            self.correlations[(index1, index2)] = self.correlations[(index2, index1)]
        else:
            self.correlations[(index1, index2)] = self.computeCorrelation(index1, index2)
        
        return self.correlations[(index1, index2)]

    def computeCorrelation(self, index1, index2):
        """Do actual computation of correlations.
:param index1: index of first slice
:param index2: index of second slice

:returns: cross correlation image for index1, index2
:rtype: FloatProcessor
"""
        ip1  = self.imagePlus.getStack().getProcessor(index1).convertToFloatProcessor()
        ip2  = self.imagePlus.getStack().getProcessor(index2).convertToFloatProcessor()
        pmcc = BlockPMCC(ip1, ip2, *self.offset)
        pmcc.rSignedSquare(self.radius[0], self.radius[1])
        tp = pmcc.getTargetProcessor()
        tp.min(0.0)
        return tp
        

    def correlateAll(self):
        """Compute all pairwise correlations for member imagePlus
:returns: dictionary of cross-correlation images
:rtype: dict
"""
        size = self.imagePlus.getStack().getSize()

        return self.correlateAllWithinRange(size)


    def correlateAllWithinRange(self, maximumRange):
        """Compute for each slice of member imagePlus the correlations with all other slices within a given range.
:param maximumRange: range limit for correlation calculation

:returns: dictionary of cross-correlation images
:rtype: dict
"""
        size = self.imagePlus.getStack().getSize()
        for index1 in xrange(1, size+1):
            for index2 in xrange(1, size+1):
                if int(math.fabs(index2-index1)) > maximumRange:
                    continue
                self.correlate(index1, index2)
                
        return self.correlations
    

    def toStack(self, index):
        """Collect all cross-correlation entries for index into an ImageStack
:param index: index for which cross-correlations are to be extracted.

:returns: ImageStack containing all computed cross-correlations for index
:rtype: ImageStack
"""
        size = self.imagePlus.getStack().getSize()
        width = self.imagePlus.getStack().getWidth()
        height = self.imagePlus.getStack().getHeight()
        resultStack = ImageStack(width, height, size)
        for k, v in self.correlations.iteritems():
            if k[0] == index:
                resultStack.setProcessor(v, k[1])
        return resultStack, (1, size)
    

    def toStackRange(self, index, stackRange):
        """Collect all cross-corrleation entries for index within a given range into an ImageStack
:param index: index for which cross-correlations are to be extracted.
:param stackRange: maximum range within which cross-correlations are collected

:returns: tuple containing an ImageStack of cross-correlations and a pair of minimum and maximum index
:rtype: tuple
"""
        upperEnd = min(index + stackRange, self.imagePlus.getStack().getSize())
        lowerEnd = max(index - stackRange, 1)
        size = upperEnd - lowerEnd + 1 # min(self.imagePlus.getStack().getSize(), 2*stackRange+1)
        width = self.imagePlus.getStack().getWidth()
        height = self.imagePlus.getStack().getHeight()
        resultStack = ImageStack(width, height, size)
        for k, v in self.correlations.iteritems():
            if k[0] == index:
                # if int(math.fabs(k[1] - index)) > stackRange:
                # continue
                if k[1] > upperEnd or k[1] < lowerEnd:
                    continue
                putIndex = k[1] + 1 - lowerEnd
                resultStack.setProcessor(v, putIndex)
        return resultStack, (lowerEnd, upperEnd)
