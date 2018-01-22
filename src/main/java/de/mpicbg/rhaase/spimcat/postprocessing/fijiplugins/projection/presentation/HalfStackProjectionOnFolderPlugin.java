package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection.presentation;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsShowingTheResult;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.AllowsSilentProcessing;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.DeliversOutputImage;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.api.TakesAnInputImage;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */

@Plugin(type = Command.class, menuPath = "XWing>Presentation>Half stack projection on files in a folder")
public class HalfStackProjectionOnFolderPlugin implements Command {
    @Override
    public void run() {
        GenericDialogPlus gd = new GenericDialogPlus("Half stack projection on a folder");
        gd.addDirectoryField("Input directory", "");
        gd.addDirectoryField("Output directory", "");

        gd.addNumericField("First file index (Zero-based)", 0, 0);
        gd.addNumericField("Last file index (Zero-based)", 50, 0);

        gd.addNumericField("File step (1 to process all files, 2 to process every second...)", 1, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String inputFolder = gd.getNextString();
        String outputFolder = gd.getNextString();
        int firstFileIndex = (int)gd.getNextNumber();
        int lastFileIndex = (int)gd.getNextNumber();
        int fileStep = (int)gd.getNextNumber();

        processFolder(inputFolder, outputFolder, firstFileIndex, lastFileIndex, fileStep, new HalfStackProjectionPlugin());

    }

    public static boolean processFolder(String sourceFolder, String targetFolder, int firstFileIndex, int lastFileIndex, int stepFileIndex, Command command)
    {
        if (!(sourceFolder.endsWith("/") || sourceFolder.endsWith("\\"))) {
            sourceFolder = sourceFolder + "/";
        }
        if (!(targetFolder.endsWith("/") || targetFolder.endsWith("\\"))) {
            targetFolder = targetFolder + "/";
        }


        File inputDirectory = new File(sourceFolder);
        if (!inputDirectory.isDirectory()) {
            System.out.println("Is not a directory: " + sourceFolder);
            return false;
        }

        System.out.println("sourceFolder " + sourceFolder);
        System.out.println("inputDirectory " + inputDirectory);

        File[] files = inputDirectory.listFiles();
        for (int i = firstFileIndex; i < lastFileIndex && i < files.length; i = i + stepFileIndex) {
            System.out.println(sourceFolder + files[i].getName());
            IJ.open(sourceFolder + files[i].getName());
            ImagePlus inputImage = IJ.getImage();
            inputImage.show();

            if (command instanceof TakesAnInputImage) {
                System.out.println("Setting in put to "+ inputImage.getTitle() + " "  + inputImage.getNSlices());
                ((TakesAnInputImage) command).setInputImage(inputImage);
            }
            if (command instanceof AllowsShowingTheResult) {
                ((AllowsShowingTheResult) command).setShowResult(true);
            }

            command.run();

            if (command instanceof DeliversOutputImage) {
                ImagePlus result = ((DeliversOutputImage) command).getOutputImage();

                IJ.run(result,"8-bit", "");
                IJ.saveAsTiff(result, targetFolder + files[i].getName());
            }
            if (command instanceof AllowsSilentProcessing) {
                ((AllowsSilentProcessing) command).setSilent(true);
            }

        }
        if (command instanceof AllowsSilentProcessing) {
            ((AllowsSilentProcessing) command).setSilent(false);
        }
        if (command instanceof AllowsShowingTheResult) {
            ((AllowsShowingTheResult) command).setShowResult(true);
        }
        return true;
    }

    public static void main(String... args) {
        String inputFolder = "C:/structure/data/xwing/2018-01-18-16-30-25-11-Robert_CalibZAP_Wfixed/processed/tif/";
        String outputFolder = "C:/structure/data/xwing/2018-01-18-16-30-25-11-Robert_CalibZAP_Wfixed/processed/test/";


        HalfStackProjectionOnFolderPlugin.processFolder(inputFolder, outputFolder, 0, 8, 1, new HalfStackProjectionPlugin());
    }

}