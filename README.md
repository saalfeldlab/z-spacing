em-thickness-estimation

Estimate thickness of slices (or at local points) of EM data. See http://arxiv.org/abs/1411.6970 for details of the algorithm.

In order to run this software, you will first need to download this repository and build from source. 
The easiest way is to import the project as a maven project into eclipse. Eclipse and maven will then take care of the dependencies and build your project and place `em-thickness-estimation-0.0.1-SNAPSHOT.jar` into `<project-root>/target`. This jar needs to be placed in `<fiji-root>/jars` or `<fiji-root>/plugins`, e.g. on Linux:
```bash
ln -s <project-root>/target/em-thickness-estimation-0.0.1-SNAPSHOT.jar <fiji-root>/jars
```
Make sure that all your Fiji jars are up to date.

Then, for running thickness estimation, use the Fiji script editor to open
```
<project-root>/src/main/python/jython_estimate_thickness.py
```
if you like to both generate the similarity matrix for your data and run thickness estimation, or
```
<project-root>/src/main/python/estimate_from_correlation_matrix.py
```
if you have a similarity matrix at hand and would like to run thickness estimation based on that. You can start the
Fiji script editor by pressing the `]`-key in the Fiji main window.

Before hitting the run button, you should consider adjusting the parameters and options to your needs:
- root:  output directory for storing (intermediate) results
- souceFile: path to image file containing similarity matrix ( `estimate_from_correlation_matrix.py` only ) - use current image if sourceFile is empty string
- dataDir: directory containing image stack ( `jython_estimate_thickness.py` only ) - use current image if dataDir is empty string
- matrixScale: if rendering matrix to image at each iteraiton, increase resolution by this factor
- options.shiftProportion: only apply shifts by this fraction
- options.nIterations: repeat this many times
- options.withReorder: allow reordering if True
- options.minimumSectionThickness: minimum distance between adjacent sections if withReorder == False
- options.multiplierGenerationRegularizerWeight: regularize multipliers (quality measure for sections) to 1.0
- options.multiplierEstimationIterations: run inner loop for multiplier estimation this many times
- options.coordinateUpdateRegularizerWeight: regularize coordinates to initial coordinates at each update
- xyScale: scale data in x and y by this before calculating similarity matrix ( jython_estimate_thickness.py only )
- doXYScale: if true, scale data by xyScale ( jython_estimate_thickness.py only )
- correlationsRange/r: maximum distance (based on their position in the stack) between two sections whose pairwise similarities are taken into account

In our experience, `xyScale` (we used `1.0` for FIB-SEM and `0.1` for TEM) and `correlationsRange/r` are the most important parameters. All other parameters can be set to their default values in the repository.

For rendering the similarity matrix at each iteraiton, uncomment
```python
# coordinateTracker.addVisitor( matrixTracker )
# coordinateTracker.addVisitor( floorTracker )
```
