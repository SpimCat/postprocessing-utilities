package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.spotdetection;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.RGBStackMerge;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.lang.reflect.GenericArrayType;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */
@Plugin(type = Command.class, menuPath = "XWing>Quality measurement>Simple spot detection slice by slice")
public class SimpleSpotDetectionSliceBySliceCommand implements Command{

    @Parameter
    private ImagePlus image;

    private double noiseParameter = 10;

    public boolean silent = false;

    @Override
    public void run() {

        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }
        ImagePlus[] masks = new ImagePlus[image.getNSlices()];

        for (int z = 0; z < masks.length; z++) {
            image.setZ(z + 1);
            IJ.run(image, "Find Maxima...", "noise=" + noiseParameter + " output=[Point Selection]");
            IJ.run(image, "Create Mask", "");

            masks[z] = IJ.getImage();
            masks[z].hide();
        }

        ImagePlus stack = RGBStackMerge.mergeChannels(masks, false);
        stack.show();
    }

    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Simple spot detection slice by slice");
        gd.addNumericField("Noise tolerance", noiseParameter, 2);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }
        noiseParameter = gd.getNextNumber();

        return true;
    }
}
