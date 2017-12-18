package de.mpicbg.haase.xwingedfreconstruction.scijava.utilities;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.BooleanType;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public class RegionToRoiConverter<B extends BooleanType<B>> {

    RandomAccessibleInterval<B> sliceMask;

    public RegionToRoiConverter(RandomAccessibleInterval<B> mask) {
        assert mask.numDimensions() == 2;
        sliceMask = mask;
    }

    public RegionToRoiConverter(RandomAccessibleInterval<B> mask, int slice) {
        assert mask.numDimensions() == 3;
        sliceMask = Views.hyperSlice(mask, 2, slice);
    }

    public Roi getRoi() {

        ImagePlus maskImp = ImageJFunctions.wrap(sliceMask, "mask");

        ImageProcessor imageProcessor = maskImp.getProcessor();
        imageProcessor.setThreshold(0.5, 258, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = new ThresholdToSelection().convert(imageProcessor);
        //maskImp.show();
        if (roi == null) {
            System.out.println("roi == null");
            return null;
        }

        int x = roi.getBounds().x + (int)sliceMask.min(0);
        int y = roi.getBounds().y + (int)sliceMask.min(1);
        roi.setLocation(x, y);
        return roi;
    }
}
