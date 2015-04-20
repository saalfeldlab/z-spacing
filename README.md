# Z-Spacing Correction

Estimate the positions and spacing between sections (or at local points) of three dimensional image data. This method may be applied to any imaging modality that acquires 3-dimensional data as a stack of 2-dimensional sections. We provide plugins for both Fiji and TrakEM2.

## Citation
Please note that the z-spacing correction plugin available through Fiji, is based on a publication. If you use it successfully for your research please cite our work:

P. Hanslovsky, J. Bogovic, S. Saalfeld (2015) Post-acquisition image based compensation for thickness variation in microscopy section series, In ''International Symposium on Biomedical Imaging (ISBI'15)'', New York [http://arxiv.org/abs/1411.6970]

## Introduction
Serial section Microscopy, using either optical or physical sectioning, is an established method for volumetric anatomy reconstruction.  Section series imaged with Electron Microscopy are currently vital for the reconstruction of the synaptic connectivity of entire animal brains such as that of *Drosophila melanogaster*.  The process of removing ultrathin layers from a solid block containing the specimen, however, is a fragile procedure and has limited precision with respect to section thickness.  Optical sectioning techniques often suffer from increasing distortion as sections deeper inside the tissue are imaged.  On summary, section thickness that is supposed to be constant, in practice is not and has to be corrected where precise measurement is desired.  We have developed a method to estimate the relative *z*-position of each individual section as a function of signal change across the section series.  The [Fiji](http://fiji.sc) plugin **Transform** > **Z-Spacing Correction** and the [TrakEM2](http://fiji.sc/TrakEM2) plugin **Plugins** > **LayerZPosition** implement this method.

## Parameters
<dl>
  <dt>Neighborhood range</dt>
  <dd>Specifies the neighborhood around each section for which pairwise similarities are calculated.</dd>
  <dt>Outer iterations</dt>
  <dd>Specifies the number of iterations in the outer loop of the optimizer.</dd>
  <dt>Outer regularization</dt>
  <dd>Specifies the amount of regularization in the outer loop of the optimizer. 0 means no regularization, 1 means full regularization (no change).  The regularizer in the outer loop damps the updates during each iteration by the specified fraction.</dd>
  <dt>Inner Iterations</dt>
  <dd>Specifies the number of iterations in inner loops of the optimizer.</dd>
  <dt>Inner Regularization</dt>
  <dd>Specifies the amount of regularization in the outer loop of the optimizer. 0 means no regularization, 1 means full regularization (no change).  The per-section quality weight requires regularization to avoid trivial solutions.  We use a Tikhonov regularizer towards 1.0 weight.</dd>
  <dt>Allow reordering</dt>
  <dd>Specifies whether layers/ sections can change their relative order in the series.</dd>
<dl>
