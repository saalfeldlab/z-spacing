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

import numpy as np
import matplotlib.animation as animation
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import os
import sys

class Experiment( object ):
    def __init__( self, damping, regularization, iterations, base_dir ):
        super( Experiment, self ).__init__()
        self.damping = damping
        self.regularization = regularization
        self.iterations = iterations
        self.base_dir = base_dir
        self.avg_shifts = np.genfromtxt( '%s/average-shifts/shift' % base_dir )

    def avg_shift( self, t ):
        return self.avg_shifts[ t, ... ]

    def lut( self, t ):
        return np.genfromtxt( '%s/lut/%04d.csv' % ( self.base_dir, t ), delimiter=',' )[:,1]



class SubplotAnimation(animation.TimedAnimation):
    def __init__( self, dampings, regularizations, pattern, iterations, **kwargs ):


	    
        self.fig = plt.figure( figsize=(12,12))
        # self.fig.suptitle( '0' )

        total = gridspec.GridSpec( 2, 1, height_ratios = ( 10, 1 ) )

        outer = gridspec.GridSpec( len( dampings ), len( regularizations ), wspace=0.1, hspace=0.3 )

        def make_inner( subplot_spec ):
            inner_grid = gridspec.GridSpecFromSubplotSpec(2, 1, subplot_spec=subplot_spec, height_ratios=(2,1) )
            ax1 = plt.Subplot( self.fig, inner_grid[ 0 ] )
            ax2 = plt.Subplot( self.fig, inner_grid[ 1 ] )

            self.fig.add_subplot( ax1 )
            self.fig.add_subplot( ax2 )

            return ax1, ax2

        self.data =[(
            Experiment( damping, regularization, iterations, pattern % ( damping, regularization ) ),
            # self.fig.add_subplot( len( regularizations ), len( dampings ), i0 * len( regularizations ) + ( i1+1 ) ),
            make_inner( outer[  i1 * len( regularizations ) + ( i0 ) ] ),
            Line2D( [], [], color='blue', alpha=0.7 ),
            Line2D( [], [], color='cyan', alpha=0.3 ),
            Line2D( [], [], color='magenta', alpha=0.9 )
            )
	        for i1, damping in enumerate( dampings ) for i0, regularization in enumerate( regularizations ) ]

        n_sections = self.data[ 0 ][ 0 ].lut( 0 ).size

        self.n_avgs = min( 100, iterations )
        self.current = 0

        min_maxes = np.array( [ ( d[ 0 ].avg_shifts.min(), d[ 0 ].avg_shifts.max() ) for d in self.data ] )
        min_max = ( np.min( min_maxes), np.max( min_maxes ) )

        for d in self.data:
            d[ 2 ].set_xdata( np.arange( n_sections ) )
            d[ 2 ].set_ydata( np.arange( n_sections ) )
            d[ 3 ].set_xdata( np.arange( iterations ) )
            d[ 3 ].set_ydata( d[ 0 ].avg_shifts )
            d[ 1 ][ 0 ].plot( np.arange( n_sections ), np.arange( n_sections ), color='black', alpha=0.3 )
            d[ 1 ][ 0 ].get_xaxis().set_ticks( [] )
            d[ 1 ][ 0 ].get_yaxis().set_ticks( [] )
            d[ 1 ][ 0 ].add_line( d[ 2 ] )
            d[ 1 ][ 0 ].set_xlim( 0, n_sections )
            d[ 1 ][ 0 ].set_ylim( 0, n_sections )
            d[ 1 ][ 1 ].set_xlim( 0, iterations )
            d[ 1 ][ 1 ].set_ylim( *min_max )
            d[ 1 ][ 1 ].add_line( d[ 3 ] )
            d[ 1 ][ 1 ].add_line( d[ 4 ] )

            if d[ 0 ].damping == dampings[ 0 ]:
                d[ 1 ][ 0 ].set_xlabel( 'reg=%.1f' % d[ 0 ].regularization )
                d[ 1 ][ 0 ].xaxis.set_label_position( 'top' )

            if d[ 0 ].regularization == regularizations[ 0 ]:
                d[ 1 ][ 0 ].set_ylabel( '%.1f' % d[ 0 ].damping )

        self.t = np.arange( iterations )

        animation.TimedAnimation.__init__(self, self.fig, **kwargs )

    def _draw_frame(self, framedata):

        titles = []

        

        min_val = max( 0, framedata - self.n_avgs + 1 )

        for d in self.data:
            data = d[ 0 ].lut( framedata )
            d[ 2 ].set_xdata( np.arange( data.size ) )
            d[ 2 ].set_ydata( data )
            avgs = d[ 0 ].avg_shifts[ min_val:framedata+1]
            ts = np.arange( min_val, framedata+1)
            # d[ 1 ][ 1 ].set_xlim( min_val, framedata )
            d[ 4 ].set_xdata( ts )
            d[ 4 ].set_ydata( avgs )


        self._drawn_artists = [ d[ 2 ] for d in self.data ] + \
          [ d[ 3 ] for d in self.data ] + \
          [ d[ 4 ] for d in self.data ]
          # [ d[ 1 ][ 1 ].get_xaxis().get_ticklabels() for d in self.data ] # + [ self.text ]

        return self._drawn_artists

    def new_frame_seq(self):
        return iter(range(self.t.size))

    def _init_draw(self):
        lines = [ d[ 2 ] for d in self.data ]
        for l in lines:
            l.set_data([], [])

# pattern = '/home/phil/workspace/z-spacing-graphical-model/run-%.1f-%.1f'
pattern = os.path.expanduser( '~/z-spacing-gridsearch/%.1f-%.1f' )

dampings = np.arange( 0, 5, 1 ) / 1.0
regs = np.arange( 0, 5, 1 ) / 2.0

ani = SubplotAnimation( dampings, regs, pattern, 2001, interval=20, blit=True )
ani.save('../gridsearch.mp4', dpi=150 )
# plt.show()
