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

