package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api;

import ij.ImagePlus;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */
public interface DeliversOutputImage {
    ImagePlus getOutputImage();
}
