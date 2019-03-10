package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.visualisation;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.DeliversOutputImage;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.TakesAnInputImage;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import net.imagej.lut.LUTService;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.awt.*;

/**
 * VisualiseVectorFieldsPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 03 2019
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Visualisation>VisualiseVectorField")
public class VisualiseVectorFieldsPlugin implements Command, AllowsSilentProcessing, TakesAnInputImage, DeliversOutputImage, AllowsShowingTheResult {

    private ImagePlus inputImage;
    private ImagePlus vectorXImage;
    private ImagePlus vectorYImage;
    private ImagePlus outputImage;
    private boolean showResult = true;
    private boolean silent = false;
    private String lookupTable = "Fire";
    private double minimumLength = 2;
    private double maximumLength = 10;
    private int stepSize = 10;
    private float lineWidth = 2;
    private boolean invertVectors = true;

    private final int finalColorStep = 100;


    @Override
    public void run() {

        if (!showDialog()) {
            return;
        }

        // create LUT
        ImagePlus lutImp = NewImage.createByteImage("Untitled", (int)(maximumLength * finalColorStep), 1, 1, NewImage.FILL_RAMP);
        IJ.run(lutImp, lookupTable, "");
        //lutImp = IJ.getImage();
        IJ.run(lutImp, "RGB Color", "");
        //lutImp = IJ.getImage();
        IJ.run(lutImp, "RGB Stack", "");
        //lutImp = IJ.getImage();
        //lutImp.show();

        lutImp.setC(1);
        byte[] pixelsR = (byte[]) lutImp.getProcessor().getPixels();
        lutImp.setC(2);
        byte[] pixelsG = (byte[]) lutImp.getProcessor().getPixels();
        lutImp.setC(3);
        byte[] pixelsB = (byte[]) lutImp.getProcessor().getPixels();






        // read image
        ImageProcessor ipVectorX = vectorXImage.getProcessor();
        ImageProcessor ipVectorY = vectorYImage.getProcessor();
        ImagePlus imp = new Duplicator().run(inputImage);
        Overlay overlay = imp.getOverlay();
        if (overlay == null) {
            overlay = new Overlay();
        }

        int stepX = stepSize;
        int stepY = stepSize;
        for (int x = 0; x < imp.getWidth(); x+=stepX) {
            for (int y = 0; y < imp.getHeight(); y+=stepY) {
                float deltaX = ipVectorX.getf(x, y);
                float deltaY = ipVectorY.getf(x, y);
                if (invertVectors){
                    deltaX = -deltaX;
                    deltaY = -deltaY;
                }

                int length = (int) (Math.sqrt(
                        Math.pow(deltaX,2) +
                        Math.pow(deltaY,2)) * finalColorStep);

                if (length > minimumLength * finalColorStep) {
                    if (length >= pixelsR.length) {
                        length = pixelsR.length - 1;
                    }
                    int r = pixelsR[length] & 0xFF;
                    int g = pixelsG[length] & 0xFF;
                    int b = pixelsB[length] & 0xFF;


                    //System.out.println("R/G/B " + r + "/" + g + "/" + b);
                    Color color = new Color(r, g, b);

                    //System.out.println(length);

                    Line line = new Line(x, y, x + deltaX, y + deltaY);
                    line.setStrokeColor(color);
                    BasicStroke stroke = new BasicStroke(lineWidth);
                    line.setStroke(stroke);
                    overlay.add(line);
                }
            }
        }
        imp.setOverlay(overlay);
        imp.show();
        IJ.run(imp, "Flatten", "");
        imp.show();
        outputImage = imp;

        if (showResult) {
            outputImage.show();
        }
    }

    private boolean showDialog() {
        if (silent) {
            return true;
        }
        String defaultImage = IJ.getImage().getTitle();

        GenericDialogPlus gd = new GenericDialogPlus("Visualise Vector Field");
        gd.addImageChoice("Target image", defaultImage);
        gd.addImageChoice("Vector_X image", defaultImage);
        gd.addImageChoice("Vector_Y image", defaultImage);

        gd.addStringField("Lookup_table", lookupTable);

        gd.addNumericField("Minimum_length (in pixels, ignore below)", minimumLength, 2);
        gd.addNumericField("Maximum_length (in pixels, just for colouring)", maximumLength, 2);

        gd.addNumericField("Step_size", stepSize, 0);
        gd.addNumericField("Line_width", lineWidth, 2);

        gd.addCheckbox("Invert vectors", invertVectors);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        inputImage = gd.getNextImage();
        vectorXImage = gd.getNextImage();
        vectorYImage = gd.getNextImage();

        lookupTable = gd.getNextString();

        minimumLength = gd.getNextNumber();
        maximumLength = gd.getNextNumber();

        stepSize = (int)gd.getNextNumber();
        lineWidth = (float)gd.getNextNumber();

        invertVectors = gd.getNextBoolean();
        return true;
    }


    @Override
    public void setInputImage(ImagePlus inputImage) {
        this.inputImage = inputImage;
    }

    @Override
    public ImagePlus getOutputImage() {
        return outputImage;
    }

    @Override
    public void setSilent(boolean value) {
        silent = value;
    }

    @Override
    public void setShowResult(boolean value) {
        showResult = value;
    }

    public static void main(String... args) {
        new ImageJ();

        VisualiseVectorFieldsPlugin vvpd = new VisualiseVectorFieldsPlugin();
        vvpd.inputImage = IJ.openImage("C:\\structure\\data\\PIV\\i2_lucerne\\000065.tif");
        vvpd.vectorXImage = IJ.openImage("C:\\structure\\data\\PIV\\deltaX\\000065.tif");
        vvpd.vectorYImage = IJ.openImage("C:\\structure\\data\\PIV\\deltaY\\000065.tif");
        vvpd.setSilent(true);
        vvpd.setShowResult(true);
        vvpd.maximumLength = 3;
        vvpd.run();

    }

}
