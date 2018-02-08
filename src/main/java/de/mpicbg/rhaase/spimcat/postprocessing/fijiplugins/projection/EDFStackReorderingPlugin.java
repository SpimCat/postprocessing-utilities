package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.DeliversOutputImage;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.TakesAnInputImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.process.ImageProcessor;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */
@Plugin(type = Command.class, menuPath = "XWing>EDF projection>EDF stack DI-ID reordering")
public class EDFStackReorderingPlugin implements Command, TakesAnInputImage, AllowsSilentProcessing, DeliversOutputImage, AllowsShowingTheResult
{    @Parameter
    ImagePlus inputImage;

    ImagePlus outputImage;


    int numberOfDetectionPlanes = 100;
    int numberOfIlluminationPlanesPerDetectionPlane = 11;

    public static boolean silent = false;
    public static boolean showResult = true;

    @Override
    public void run() {
        if (!silent) {
            if (!showDialog()) {
                return;
            }
        }

        inputImage.killRoi();
        ImagePlus inputImage = new Duplicator().run(this.inputImage, 1, this.inputImage.getNSlices());

//        ImageProcessor[] processorArray = new ImageProcessor[inputImage.getNSlices()];
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight());

        ImageProcessor emptySlice = NewImage.createImage("empty", inputImage.getWidth(), inputImage.getHeight(), 1, inputImage.getBitDepth(), NewImage.FILL_BLACK).getProcessor();
//        int count = 0;
        for (int rid = -numberOfIlluminationPlanesPerDetectionPlane / 2; rid <= numberOfIlluminationPlanesPerDetectionPlane / 2; rid++) {
            for (int riz = 0; riz < numberOfDetectionPlanes; riz++) {
                int blockNumber = (riz + rid) * numberOfIlluminationPlanesPerDetectionPlane;
                int slice = blockNumber + (numberOfIlluminationPlanesPerDetectionPlane / 2) - rid;
                if (slice < 0 || slice >= inputImage.getNSlices()) {
                    //slice = 0;
                    stack.addSlice(emptySlice);
                } else {
                    inputImage.setZ(slice + 1);
                    stack.addSlice(inputImage.getProcessor());
                }
//                processorArray[count] = inputImage.getProcessor();


                //System.out.println("C " + slice + " [B]\t=\t" + (riz + rid) + "\t" + riz);
            }
        }

        ImagePlus resultImage = new ImagePlus("Reordered " + inputImage.getTitle(), stack);
        outputImage = HyperStackConverter.toHyperStack(resultImage, 1, numberOfDetectionPlanes, numberOfIlluminationPlanesPerDetectionPlane);

        if (showResult) {
            outputImage.show();
        }
    }

    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("EDF stack DI-ID reordering");
        gd.addMessage("This plugin reorders stacks which were recorded like\nD0L1,D0I1,D0I2, D1L1,D1I1,D1I2\nto a stack like this:\nD0L0,D1l0,D0L1,D1l1,D0L2,D1l2");
        gd.addNumericField("Number of detection planes", numberOfDetectionPlanes, 0);
        gd.addNumericField("Number of illumination planes per detection plane", numberOfIlluminationPlanesPerDetectionPlane, 0);
        gd.addMessage("Hint: Both numbers must be equal to the number of slices: " + inputImage.getNSlices());
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }
        numberOfDetectionPlanes = (int)gd.getNextNumber();
        numberOfIlluminationPlanesPerDetectionPlane = (int)gd.getNextNumber();

        return true;
    }

    @Override
    public void setInputImage(ImagePlus inputImage) {
        this.inputImage = inputImage;
    }

    @Override
    public void setSilent(boolean value) {
        silent = value;
    }

    public static void main(String... args) {
        new ImageJ();

        IJ.run("Raw...", "open=C:\\structure\\data\\xwing\\2018-01-23-10-56-58-86-Robert_CalibZAP_tEDF10\\stacks\\C0L0\\000000.raw image=[16-bit Unsigned] width=1024 height=2048 number=8200 little-endian");
        ImagePlus testImage = IJ.getImage();

        EDFStackReorderingPlugin edfsrp = new EDFStackReorderingPlugin();
        edfsrp.setInputImage(testImage);
        edfsrp.run();

    /*
        int numberOfDZs = 20;
        int numberOfIZs = 11;

        ArrayList<int[]> zList = new ArrayList<int[]>();

        int count = 0;
        for (int dz = 0; dz < numberOfDZs; dz++) {
            for (int iz = 0; iz < numberOfIZs; iz++) {
                int imagedDZ = dz;
                int imagedIZ = dz + iz - numberOfIZs / 2;
                zList.add(new int[]{imagedDZ, imagedIZ});

                if (imagedIZ == 8) {
                    System.out.println("I " + count + " [B" + (dz * numberOfIZs) + "]\t=\t" + imagedDZ + "\t" + imagedIZ);
                }
                count++;
            }
        }

        //for (int riz = 0; riz < numberOfDZs; riz++) {
        int riz = 8;
        for (int rid = -numberOfIZs / 2; rid <= numberOfIZs / 2; rid++) {
            int blockNumber = (riz + rid) * numberOfIZs;
            int slice = blockNumber + (numberOfIZs / 2) - rid;

            //int slice = (riz + rid) * (numberOfIZs) + (rid - (numberOfIZs / 2));

            System.out.println("C " + slice + " [B]\t=\t" + (riz + rid) + "\t" + riz);
        }
        //}

    */




    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    @Override
    public ImagePlus getOutputImage() {
        return outputImage;
    }
}