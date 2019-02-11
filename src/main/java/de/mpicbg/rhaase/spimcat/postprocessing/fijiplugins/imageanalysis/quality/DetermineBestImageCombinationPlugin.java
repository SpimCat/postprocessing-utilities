package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.quality;

import autopilot.measures.FocusMeasures;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection.TenengradFusionPlugin;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.plugin.RGBStackMerge;
import sun.rmi.transport.tcp.TCPEndpoint;

import java.io.IOException;

/**
 * DetermineBestImageCombinationPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 11 2018
 */
public class DetermineBestImageCombinationPlugin {

    static String inputFolder = "C:/Users/rhaase/Desktop/2018-11-12-14-48-49-91-Simulation_angle2/processed/";
            //"C:/Users/rhaase/Desktop/2018-01-09-16-55-18-63-CalibZAP_Unfused/";
    static String resultFolder = "C:/Users/rhaase/Desktop/2018-11-12-14-48-49-91-Simulation_angle2/processed/results/";
            //"C:/Users/rhaase/Desktop/2018-01-09-16-55-18-63-CalibZAP_Unfused/results/";


    public static void main(String... args) {
        new ImageJ();
        new DetermineBestImageCombinationPlugin().process();
    }

    private void test(String shortName, ImagePlus[] images, FocusMeasures.FocusMeasure focusMeasure) {
        ImagePlus fused = images[0];

        if (images.length > 1) {
            fused = new RGBStackMerge().mergeHyperstacks(images, true);
            fused = HyperStackConverter.toHyperStack(fused, 1, images.length, 1);
        }
        fused.show();

        //IJ.run(fused, "Tenengrad fusion", "blur=3 blur_0=3 blur_1=1");
        ImagePlus tenengrad = new TenengradFusionPlugin().process(fused);

        IJ.run(tenengrad, "Enhance Contrast", "saturated=0.35");

        IJ.saveAs(tenengrad, "tiff", resultFolder +  shortName + ".tif");
        IJ.saveAs(tenengrad, "jpeg", resultFolder +  shortName + ".jpg");
//
//        MeasureQualityPerSlicePlugin mqpsp = new MeasureQualityPerSlicePlugin(tenengrad);
//        mqpsp.setSilent(true);
//        mqpsp.setShowResult(false);
//        mqpsp.setShowPlots(false);
//
//        double[] quality = mqpsp.analyseFocusMeasure(focusMeasure);
//
        MeasureQualityInTilesPlugin mqitp = new MeasureQualityInTilesPlugin(tenengrad, 16, 32);
        mqitp.setSilent(true);
        mqitp.setShowResult(false);
        ImagePlus qualityTiles = mqitp.analyseFocusMeasure(focusMeasure);
        double averageTileQuality = qualityTiles.getStatistics().mean;

        // IJ.run(tenengrad, "Image Focus Measurements slice by slice (Adapted Autopilot code, Royer et Al. 2016)", "normalized_dct_shannon_entropy currentdata=DUP_img");

        tenengrad.show();
        ResultsTable table = ResultsTable.getResultsTable();
        //table.incrementCounter();
        table.setValue("Tile quality", table.getCounter(), averageTileQuality);
        table.setValue("Test", table.getCounter() - 1, shortName);
        table.setValue("Metric", table.getCounter() - 1, focusMeasure.toString());
        table.show("Results");
    }

    private void process() {

        FocusMeasures.FocusMeasure[] focusMeasures = {
                FocusMeasures.FocusMeasure.StatisticMean,
                FocusMeasures.FocusMeasure.SpectralNormDCTEntropyShannon,
                FocusMeasures.FocusMeasure.DifferentialTenengrad,
                FocusMeasures.FocusMeasure.SpectralDFTHighLowFreqRatio,
                FocusMeasures.FocusMeasure.SpectralDCTHighLowFreqRatio,

        };

        for (FocusMeasures.FocusMeasure focusMeasure : focusMeasures) {
            //FocusMeasures.FocusMeasure.SpectralNormDCTEntropyShannon;

            ImagePlus l0 = IJ.openImage(inputFolder + "L0.tif");
            ImagePlus l1 = IJ.openImage(inputFolder + "L1.tif");
            ImagePlus l2 = IJ.openImage(inputFolder + "L2.tif");
            ImagePlus l3 = IJ.openImage(inputFolder + "L3.tif");

            test("L0", new ImagePlus[]{l0, l0}, focusMeasure);
            test("L1", new ImagePlus[]{l1, l1}, focusMeasure);
            test("L2", new ImagePlus[]{l2, l2}, focusMeasure);
            test("L3", new ImagePlus[]{l3, l3}, focusMeasure);

            test("L01", new ImagePlus[]{l0, l1}, focusMeasure);
            test("L02", new ImagePlus[]{l0, l2}, focusMeasure);
            test("L03", new ImagePlus[]{l0, l3}, focusMeasure);
            test("L12", new ImagePlus[]{l1, l2}, focusMeasure);
            test("L13", new ImagePlus[]{l1, l3}, focusMeasure);
            test("L23", new ImagePlus[]{l2, l3}, focusMeasure);

            test("L012", new ImagePlus[]{l0, l1, l2}, focusMeasure);
            test("L013", new ImagePlus[]{l0, l1, l3}, focusMeasure);
            test("L023", new ImagePlus[]{l0, l2, l3}, focusMeasure);
            test("L123", new ImagePlus[]{l1, l2, l3}, focusMeasure);

            test("L0123", new ImagePlus[]{l0, l1, l2, l3}, focusMeasure);

            IJ.run("Close All");

        }
        ResultsTable table = ResultsTable.getResultsTable();
        try {
            table.saveAs(resultFolder + "quality.xls");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
