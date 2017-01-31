#!/usr/bin/env python

import numpy as np
import matplotlib.pyplot as plt

# compare shift collection and mediation for commits
# 7fc6e29ec76feba42f628846c810228c9bbf60be
# 1e4b2ee90dae0d75b458116eb5e6ed278eeebaed

# csv format:
# collection time, mediation time

def make_hist(data, **kwargs):
	means = np.array([np.mean(x) for x in data])
	stds =  np.array([np.std(x) for x in data])
	m = np.min(means-1*stds)
	M = np.max(means+1*stds)

	return plt.hist(data, range=(m, M), **kwargs)

trove_data = np.genfromtxt('with-trove.csv', delimiter=',') / 1e9
map_data = np.genfromtxt('with-map.csv', delimiter=',') / 1e9

ax = plt.subplot(1, 2, 1)
make_hist([map_data[100:,0], trove_data[100:,0]], label=('map', 'trove'))
plt.legend()

ax = plt.subplot(1, 2, 2)
make_hist([map_data[100:,1], trove_data[100:,1]], label=('map', 'trove'))
plt.legend()


plt.show()
