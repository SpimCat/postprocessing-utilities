package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imagemath;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import edu.mines.jtk.dsp.BandPassFilter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.ShapeRoi;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.FFTFilter;
import net.imagej.ops.Ops;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * BandPassFilterPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 09 2018
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Internal (experimental)>Band pass filter")
public class BandPassFilterPlugin implements Command, AllowsShowingTheResult, AllowsSilentProcessing {
    @Parameter
    ImagePlus input;

    int highFrequencyThreshold = 1;
    int lowFrequencyThreshold = 1;


    ImagePlus output;


    boolean showResult = true;
    boolean silent = false;

    @Deprecated
    public BandPassFilterPlugin() {
        super();
    }

    public BandPassFilterPlugin(ImagePlus input, int lowFrequencyThreshold, int highFrequencyThreshold) {
        this.input = input;
        this.lowFrequencyThreshold = lowFrequencyThreshold;
        this.highFrequencyThreshold = highFrequencyThreshold;
    }


    @Override
    public void run() {
        if (!silent)  {
            if (!showDialog()) {
                return;
            }
        }

        if (highFrequencyThreshold > input.getWidth() / 2) {
            highFrequencyThreshold = input.getWidth() / 2;
        }

        ImagePlus[] images = new ImagePlus[input.getNSlices()];

        for (int i = 0; i < images.length; i++) {
            input.setZ( i + 1);
            IJ.run(input, "FFT", "");
            ImagePlus imp = IJ.getImage();
            imp.hide();

            int centerX = input.getWidth() / 2;
            int centerY = input.getHeight() / 2;

            if (lowFrequencyThreshold == 0) {
                OvalRoi outerRoi = new OvalRoi(centerX - highFrequencyThreshold, centerY - highFrequencyThreshold, highFrequencyThreshold * 2, highFrequencyThreshold * 2);
                imp.setRoi(outerRoi);
            } else {
                OvalRoi outerRoi = new OvalRoi(centerX - highFrequencyThreshold, centerY - highFrequencyThreshold, highFrequencyThreshold * 2, highFrequencyThreshold * 2);
                OvalRoi innerRoi = new OvalRoi(centerX - lowFrequencyThreshold, centerY - lowFrequencyThreshold, lowFrequencyThreshold * 2, lowFrequencyThreshold * 2);

                ShapeRoi bandRoi = new ShapeRoi(outerRoi).not(new ShapeRoi(innerRoi));
                imp.setRoi(bandRoi);
            }
            IJ.run(imp, "Make Inverse", "");

            //imp.setRoi(new OvalRoi(114,115,29,26));
            IJ.run(imp, "Multiply...", "value=0");
            imp.killRoi();

            IJ.run(imp, "Inverse FFT", "");

            images[i] = IJ.getImage();
            images[i].hide();
        }

        output = RGBStackMerge.mergeChannels(images, false);
        if (showResult) {
            output.show();
        }
    }

    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Band Pass Filter");
        gd.addNumericField("Low frequency threshold", lowFrequencyThreshold,0);
        gd.addNumericField("High frequency threshold", highFrequencyThreshold, 0);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return false;
        }

        lowFrequencyThreshold = (int)gd.getNextNumber();
        highFrequencyThreshold = (int)gd.getNextNumber();

        return true;
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    @Override
    public void setSilent(boolean value) {
        silent = value;
    }

    public static void main(String... arg) {
        new ij.ImageJ();

        ImagePlus
                input = IJ.openImage("C:/structure/data/fish_GAFASO.tif");
        input.show();

        new BandPassFilterPlugin(input, 50, Integer.MAX_VALUE).run();
    }
}
