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
import matplotlib.widgets as widgets
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

class DiscreteSlider( widgets.Slider ):
    """A matplotlib slider widget with discrete steps."""
    def __init__(self, *args, **kwargs):
        """Identical to Slider.__init__, except for the "increment" kwarg.
        "increment" specifies the step size that the slider will be discritized
        to."""
        self.inc = kwargs.pop('increment', 1)
        widgets.Slider.__init__(self, *args, **kwargs)

    def set_val(self, val):
        discrete_val = int(val / self.inc) * self.inc
        # We can't just call Slider.set_val(self, discrete_val), because this 
        # will prevent the slider from updating properly (it will get stuck at
        # the first step and not "slide"). Instead, we'll keep track of the
        # the continuous value as self.val and pass in the discrete value to
        # everything else.
        xy = self.poly.xy
        xy[2] = discrete_val, 1
        xy[3] = discrete_val, 0
        self.poly.xy = xy
        self.valtext.set_text(self.valfmt % discrete_val)
        if self.drawon: 
            self.ax.figure.canvas.draw()
        self.val = val
        if not self.eventson: 
            return
        for cid, func in self.observers.items():
            func(discrete_val)



class SubplotAnimation():
    def __init__( self, dampings, regularizations, pattern, iterations, **kwargs ):


	    
        self.fig = plt.figure( figsize=(12,12))
        self.sliderax = self.fig.add_axes( [ 0.2, 0.02, 0.6, 0.03 ], axisbg='yellow' )
        # self.fig.suptitle( '0' )

        outer = gridspec.GridSpec( len( regularizations ), len( dampings ), wspace=0.1, hspace=0.3 )
        self.outer = outer

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
            make_inner( outer[  i0 * len( regularizations ) + ( i1 ) ] ),
            Line2D( [], [], color='blue', alpha=0.7 ),
            Line2D( [], [], color='orange', alpha=0.7 )
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
            d[ 3 ].set_xdata( [ 0 ] )
            d[ 3 ].set_ydata( [ 0 ] )
            d[ 1 ][ 0 ].plot( np.arange( n_sections ), np.arange( n_sections ), color='black', alpha=0.3 )
            d[ 1 ][ 0 ].get_xaxis().set_ticks( [] )
            d[ 1 ][ 0 ].get_yaxis().set_ticks( [] )
            d[ 1 ][ 0 ].add_line( d[ 2 ] )
            d[ 1 ][ 0 ].set_xlim( 0, n_sections )
            d[ 1 ][ 0 ].set_ylim( 0, n_sections )
            d[ 1 ][ 1 ].set_xlim(  -self.n_avgs, 0 )
            d[ 1 ][ 1 ].set_ylim( *min_max )
            d[ 1 ][ 1 ].add_line( d[ 3 ] )

        self.t = np.arange( iterations )

        self.slider = DiscreteSlider( self.sliderax, 't=', 0, iterations, valinit=0, increment=1 )
        self.slider.on_changed( self._draw_frame )

        # animation.TimedAnimation.__init__(self, self.fig, **kwargs )

    def _draw_frame(self, framedata):

        # print( "DRAWING FRAME: ", framedata )

        titles = []

        framedata = int( framedata )

        

        min_val = max( 0, framedata - self.n_avgs + 1 )

        for d in self.data:
            data = d[ 0 ].lut( framedata )
            d[ 2 ].set_xdata( np.arange( data.size ) )
            d[ 2 ].set_ydata( data )
            avgs = d[ 0 ].avg_shifts[ min_val:framedata+1]
            ts = np.arange( min_val, framedata+1)
            d[ 1 ][ 1 ].set_xlim( min_val, framedata )
            d[ 3 ].set_xdata( ts )
            d[ 3 ].set_ydata( avgs )


        self._drawn_artists = [ d[ 2 ] for d in self.data ] + \
          [ d[ 3 ] for d in self.data ] + \
          []

        self.fig.canvas.draw()
          # [ d[ 1 ][ 1 ].get_xaxis().get_ticklabels() for d in self.data ] # + [ self.text ]

    # def new_frame_seq(self):
    #     return iter(range(self.t.size))

    # def _init_draw(self):
    #     lines = [ d[ 2 ] for d in self.data ]
    #     for l in lines:
    #         l.set_data([], [])

# pattern = '/home/phil/workspace/z-spacing-graphical-model/run-%.1f-%.1f'
pattern = os.path.expanduser( '~/z-spacing-gridsearch/%.1f-%.1f' )

dampings = np.arange( 0, 5, 1 ) / 1.0
regs = np.arange( 0, 5, 1 ) / 2.0

ani = SubplotAnimation( dampings, regs, pattern, 2001 )
# ani.save('../gridsearch.mp4', dpi=150 )
plt.show()
