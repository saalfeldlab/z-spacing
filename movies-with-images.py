"""
=================
Animated subplots
=================

This example uses subclassing, but there is no reason that the proper function
couldn't be set up and then use FuncAnimation. The code is long, but not
really complex. The length is due solely to the fact that there are a total of
9 lines that need to be changed for the animation as well as 3 subplots that
need initial set up.

"""

# export CLASSPATH=`ls ~/local/Fiji.app/jars/*.jar -1 | tr '\n' ':'`
# export CLASSPATH=`ls ~/local/Fiji.app/plugins/*.jar -1 | tr '\n' ':'`:$CLASSPATH

import numpy as np
import matplotlib.animation as animation
import matplotlib.gridspec as gridspec
import matplotlib.image as image
import matplotlib.markers as markers
import matplotlib.pyplot as plt
import matplotlib.widgets as widgets
from matplotlib.lines import Line2D
import os
import scipy.ndimage.interpolation as interpolation
import skimage.transform as transform
import sys
import vigra

import imglyb

from imglyb import util

from jnius import autoclass

LutRealTransform = autoclass( 'org.janelia.thickness.lut.LUTRealTransform' )
MatrixStripConversion = autoclass( 'org.janelia.utility.MatrixStripConversion' )
NLinearInterpolatorFactory = autoclass( 'net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory' )
RealViews = autoclass( 'net.imglib2.realtransform.RealViews' )


class Experiment( object ):
   
    def __init__( self, damping, regularization, iterations, base_dir ):
        super( Experiment, self ).__init__()
        self.damping = damping
        self.regularization = regularization
        self.iterations = iterations
        self.base_dir = base_dir
        self.avg_shifts = np.genfromtxt( '%s/average-shifts/shift' % base_dir )
        self.mat0 = np.ascontiguousarray( vigra.readImage( '%s/matrices/%04d.tif' % ( self.base_dir, 0 ) ).squeeze().transpose() )

    def avg_shift( self, t ):
        return self.avg_shifts[ t, ... ]

    def lut( self, t ):
        return np.genfromtxt( '%s/lut/%04d.csv' % ( self.base_dir, t ), delimiter=',' )[:,1]

    
    def matrix( self, t ):
        mat_imglib = util.to_imglib( self.mat0 )
        mat_type = mat_imglib.randomAccess().get()
        mat_type.setReal( np.nan )
        matrix = MatrixStripConversion.stripToMatrix( mat_imglib, mat_type )

        tf = LutRealTransform( self.lut( t ).tolist(), 2, 2 )
        extended = util.Views.interpolate( util.Views.extendValue( matrix, mat_type ), NLinearInterpolatorFactory() )
        transformed = util.Views.interval( util.Views.raster( RealViews.transformReal( extended, tf ) ), matrix )

        target = np.empty( ( self.mat0.shape[ 0 ], self.mat0.shape[ 0 ] ) )
        util.Helpers.burnIn( transformed, util.to_imglib( target ) )
        return target[ ::-1, ... ]

    def matrix_no_imglib( self, t ):

        mat = np.ascontiguousarray( vigra.readImage( '%s/matrices/%04d.tif' % ( self.base_dir, t ) ).squeeze().transpose() )
        mat[ mat >= ( 1.0 - 1e-2 ) ] = np.nan
        mat[ :, mat.shape[ 1 ] // 2 ] = 1.0
        mat[ np.isnan( mat )] = 0.0
        # return mat

        transformed = np.zeros( ( mat.shape[0], mat.shape[0] + mat.shape[ 1 ] // 2 ), dtype = mat.dtype ) + np.nan
        transformed[ :, :mat.shape[ 1 ] ] = mat

        for z in range( mat.shape[ 0 ] ):
            transformed[ z, : ] = np.roll( transformed[ z, : ], z )

        return transformed[ :, ( mat.shape[ 1 ] // 2 ) : ][ ::-1, :: ]

        # return np.ma.array( mat, mask=np.isnan( mat ) )
        # tf = transform.AffineTransform( shear=np.pi/2 ) # , translation=( mat.shape[ 1 ] / 2, 0 ) )
        # transformed = transform.warp( mat, tf, output_shape=( mat.shape[ 0 ], mat.shape[ 0 ] ), preserve_range = True )
        # return transformed
        # return np.ma.array( transformed, mask=np.isnan( transformed ) )



class SubplotAnimation( animation.TimedAnimation ):
    def __init__( self, dampings, regularizations, pattern, iterations, **kwargs ):


	    
        self.fig = plt.figure( figsize=(12,12))
        # self.fig.suptitle( '0' )

        outer = gridspec.GridSpec( len( dampings ), len( regularizations ), wspace=0.1, hspace=0.3) #, height_ratios=(10,1) )
        self.outer = outer
        
        def make_inner( subplot_spec ):
            inner_grid = gridspec.GridSpecFromSubplotSpec(2, 2, subplot_spec=subplot_spec, height_ratios=(6,1) )
            ax1 = plt.Subplot( self.fig, inner_grid[ 0, 0 ] )
            ax2 = plt.Subplot( self.fig, inner_grid[ 0, 1 ] )
            ax3 = plt.Subplot( self.fig, inner_grid[ 1, : ] )

            self.fig.add_subplot( ax1 )
            self.fig.add_subplot( ax2 )
            self.fig.add_subplot( ax3 )

            return ax1, ax2, ax3

        self.data =[(
            Experiment( damping, regularization, iterations, pattern % ( damping, regularization ) ),
            # self.fig.add_subplot( len( regularizations ), len( dampings ), i0 * len( regularizations ) + ( i1+1 ) ),
            make_inner( outer[  i1 * len( regularizations ) + ( i0 ) ] ),
            Line2D( [], [], color='blue', alpha=0.7 ),
            Line2D( [], [], color='cyan', alpha=0.3 ),
            [],
            Line2D( [], [], color='magenta', alpha=0.7, marker=markers.CARETDOWN )
            )
	        for i1, damping in enumerate( dampings ) for i0, regularization in enumerate( regularizations ) ]

        n_sections = self.data[ 0 ][ 0 ].lut( 0 ).size

        self.n_avgs = 1 # min( 100, iterations )
        self.current = 0

        self.cmap = 'viridis'
        # self.cmap = 'Greys_r'

        min_maxes = np.array( [ ( d[ 0 ].avg_shifts.min(), d[ 0 ].avg_shifts.max() ) for d in self.data ] )
        min_max = ( np.min( min_maxes), np.max( min_maxes ) )

        for d in self.data:
            d[ 2 ].set_xdata( np.arange( n_sections ) )
            d[ 2 ].set_ydata( np.arange( n_sections ) )
            d[ 3 ].set_xdata( np.arange( iterations ) )
            d[ 3 ].set_ydata( d[ 0 ].avg_shifts[ :iterations ] )
            d[ 1 ][ 0 ].plot( np.arange( n_sections ), np.arange( n_sections ), color='black', alpha=0.3 )
            d[ 1 ][ 0 ].get_xaxis().set_ticks( [] )
            d[ 1 ][ 0 ].get_yaxis().set_ticks( [] )
            d[ 1 ][ 0 ].add_line( d[ 2 ] )
            d[ 1 ][ 0 ].set_xlim( 0, n_sections )
            d[ 1 ][ 0 ].set_ylim( 0, n_sections )
            d[ 1 ][ 2 ].set_xlim( 0, iterations )
            d[ 1 ][ 2 ].set_ylim( *min_max )
            d[ 1 ][ 2 ].add_line( d[ 3 ] )
            d[ 1 ][ 2 ].add_line( d[ 5 ] )
            d[ 4 ].append( d[ 1 ][ 1 ].imshow( d[ 0 ].matrix( 0 ), cmap=self.cmap ) ) # image.AxesImage( d[ 1 ][ 1 ] ) )
            d[ 1 ][ 1 ].get_xaxis().set_ticks( [] )
            d[ 1 ][ 1 ].get_yaxis().set_ticks( [] )
            d[ 1 ][ 1 ].get_xaxis().set_visible( False )
            d[ 1 ][ 1 ].get_yaxis().set_visible( False )

            d[ 1 ][ 0 ].set_aspect( 'equal' )
            d[ 1 ][ 1 ].set_aspect( 'equal' )

            for ax in d[ 1 ]:
                plt.sca( ax )
                # plt.axis( 'off' )

            if d[ 0 ].damping == dampings[ 0 ]:
                d[ 1 ][ 0 ].set_xlabel( 'reg=%.1f' % d[ 0 ].regularization )
                d[ 1 ][ 0 ].xaxis.set_label_position( 'top' )

            if d[ 0 ].regularization == regularizations[ 0 ]:
                d[ 1 ][ 0 ].set_ylabel( '%.1f' % d[ 0 ].damping )

        # plt.axis('off')

        self.t = np.arange( iterations )

        # self._draw_frame( 0 )

        animation.TimedAnimation.__init__( self, self.fig, **kwargs )

    def _draw_frame(self, framedata):

        # print( "DRAWING FRAME: ", framedata )

        titles = []

        framedata = int( framedata )

        

        min_val = max( 0, framedata - self.n_avgs + 1 )
        # min_val = framedata

        for d in self.data:
            data = d[ 0 ].lut( framedata )
            d[ 2 ].set_xdata( np.arange( data.size ) )
            d[ 2 ].set_ydata( data )
            avgs = d[ 0 ].avg_shifts[ framedata:framedata+1]
            ts = np.arange( min_val, framedata+1)
            d[ 5 ].set_xdata( ts )
            d[ 5 ].set_ydata( avgs )
            matrix = d[ 0 ].matrix( framedata )
            d[ 4 ][ 0 ].set_data( matrix )
            # d[ 1 ][ 1 ].imshow( matrix, cmap = self.cmap )


        self._drawn_artists = [ d[ 2 ] for d in self.data ] + \
          [ d[ 3 ] for d in self.data ] + \
          [ d[ 4 ] for d in self.data ] + \
          [ d[ 5 ] for d in self.data ]

        # for artist in self._drawn_artists:
        #     artist.draw()

        # self.fig.canvas.draw()

        return self._drawn_artists
          # [ d[ 1 ][ 1 ].get_xaxis().get_ticklabels() for d in self.data ] # + [ self.text ]

    def new_frame_seq(self):
        return iter(range(self.t.size))

    def _init_draw(self):
        lines = [ d[ 2 ] for d in self.data ]
        for l in lines:
            l.set_data([], [])

# pattern = '/home/phil/workspace/z-spacing-graphical-model/run-%.1f-%.1f'
pattern = os.path.expanduser( '~/z-spacing-gridsearch/%.1f-%.1f' )

# dampings = np.arange( 0, 5, 1 ) / 1.0
# regs = np.arange( 0, 5, 1 ) / 2.0

dampings = [ 0.0, 1.0, 2.0 ]
regs = [ 0.5, 2.0 ]

ani = SubplotAnimation( dampings, regs, pattern, 2001, blit=False, interval=20 )
ani.save('../gridsearch-render-matrices.mp4', dpi=150 )
# plt.show()
