package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import de.mpicbg.rhaase.scijava.AbstractFocusMeasuresPlugin;
import fiji.util.gui.GenericDialogPlus;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.imglib2.Cursor;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.Arrays;

/**
 * LabelingAccuracyPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 03 2019
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Quality measurement>Labeling accuracy")
public class LabelingAccuracyPlugin implements Command {

    ImagePlus input;
    ImagePlus groundTruth;
    double minimumObjectOverlap = 0.5;

    @Override
    public void run() {

        if (!showDialog()) {
            return;
        }

        double accuracy = accuracy(input, groundTruth, minimumObjectOverlap);

        ResultsTable table = ResultsTable.getResultsTable();
        table.incrementCounter();
        table.addValue("Accuracy", accuracy);
        table.show("Results");
    }

    private boolean showDialog() {
        GenericDialogPlus gd = new GenericDialogPlus("Determine accuracy of a label map");
        gd.addImageChoice("Label_map", "");
        gd.addImageChoice("Ground_truth", "");
        gd.addNumericField("Minimum_object_overlap", minimumObjectOverlap, 2);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return false;
        }

        input = gd.getNextImage();
        groundTruth = gd.getNextImage();
        minimumObjectOverlap = gd.getNextNumber();

        return true;
    }

    public static double accuracy(ImagePlus input, ImagePlus groundTruth) {
        return accuracy(input, groundTruth, 0.5);
    }

    public static double accuracy(ImagePlus input, ImagePlus groundTruth, double minimumObjectOverlap) {

        Img<FloatType> img = ImageJFunctions.convertFloat(input);
        Img<FloatType> gt = ImageJFunctions.convertFloat(groundTruth);

        Cursor<FloatType> cursor = img.cursor();
        Cursor<FloatType> cursorGT = gt.cursor();

        FloatType min = new FloatType();
        FloatType max = new FloatType();

        ComputeMinMax<FloatType> minMaxImg = new ComputeMinMax<>(img, min, max);
        minMaxImg.process();
        int maxImg = (int) max.get() + 1;

        minMaxImg = new ComputeMinMax<>(gt, min, max);
        minMaxImg.process();
        int maxGT = (int) max.get() + 1;

        long[][] counts = new long[maxImg][maxGT];
        long[] countsImg = new long[maxImg];
        long[] countsGT = new long[maxGT];



        while (cursor.hasNext() && cursorGT.hasNext()) {
            int intensityImg = (int) cursor.next().get();
            int intensityGt = (int) cursorGT.next().get();

            counts[intensityImg][intensityGt]++;
            countsImg[intensityImg]++;
            countsGT[intensityGt]++;
        }


        //for (int i = 0; i < maxImg; i++) {
        //    System.out.println(Arrays.toString(counts[i]));
        //}


        //acc = TP/(TP+FN+FP)
        // Ein TP ist eine pred instanz, die mindestens einen IoU = .5
        // gegenueber einer gt instanz besizt (d.h. das ergibt ein eindeutiges matching).
        // FP ist eine pred instanz, fuer die es kein matching in gt gibt,
        // FN eine gt instanz fuer die es keines in pred gibt.

        int tp = 0;
        int fn = 0;
        int fp = 0;
        for (int i = 1; i < maxImg; i++) {
            boolean found = false;
            for (int g = 1; g < maxGT; g++) {
                double intersection = counts[i][g];
                double union = countsImg[i] + countsGT[g] - counts[i][g];
                double jaccardIndex = intersection / union;
                if (jaccardIndex > minimumObjectOverlap) {
                    found = true;
                    break;
                }
            }
            if (found) {
                tp++;
            } else {
                fp++;
            }
        }

        for (int g = 1; g < maxGT; g++) {
            boolean found = false;
            for (int i = 1; i < maxImg; i++) {
                double intersection = counts[i][g];
                double union = countsImg[i] + countsGT[g] - counts[i][g];
                double jaccardIndex = intersection / union;
                if (jaccardIndex > minimumObjectOverlap) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fn++;
            }
        }

        System.out.println("TP " + tp);
        System.out.println("FN " + fn);
        System.out.println("FP " + fp);

        double accuracy = (double)tp / (tp + fn + fp);
        return accuracy;
    }

    public static void main(String... args) {

        int[] gt = {0,0,0,0,0,1,1,1,2,2,2,3,3,3};
        int[] i1 = {0,0,0,0,0,0,1,1,0,0,0,0,0,0};
        double acc1 = 0.333;

        int[] i2 = {0,0,0,0,0,0,1,1,0,2,2,0,3,3};
        double acc2 = 1.0;

        int[] i3 = {0,0,0,0,0,0,0,1,0,0,2,0,0,3};
        double acc3 = 0.0;


        int[] i4 = {1,2,3,0,0,0,0,2,0,0,3,0,0,1};
        double acc4 = 0.0;

        Img<IntType> imgGT = ArrayImgs.ints(gt, new long[]{gt.length});
        ImagePlus impGT = ImageJFunctions.wrap(imgGT, "GT");

        Img<IntType> img1 = ArrayImgs.ints(i1, new long[]{i1.length});
        ImagePlus imp1 = ImageJFunctions.wrap(img1, "i1");

        double acc = accuracy(imp1, impGT);
        System.out.println(acc1 + " = " + acc);

        Img<IntType> img2 = ArrayImgs.ints(i2, new long[]{i2.length});
        ImagePlus imp2 = ImageJFunctions.wrap(img2, "i2");

        acc = accuracy(imp2, impGT);
        System.out.println(acc2 + " = " + acc);


        Img<IntType> img3 = ArrayImgs.ints(i3, new long[]{i3.length});
        ImagePlus imp3 = ImageJFunctions.wrap(img3, "i3");

        acc = accuracy(imp3, impGT);
        System.out.println(acc3 + " = " + acc);


        Img<IntType> img4 = ArrayImgs.ints(i4, new long[]{i4.length});
        ImagePlus imp4 = ImageJFunctions.wrap(img4, "i4");

        acc = accuracy(imp4, impGT);
        System.out.println(acc4 + " = " + acc);


    }
}
