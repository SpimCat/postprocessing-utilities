package de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.analysis.localquality.stddev.ui;

import de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.analysis.localquality.stddev.AverageInDepthCalculator;
import de.mpicbg.haase.xwingedfreconstruction.scijava.utilities.RegionToRoiConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
@Plugin(type = Command.class, menuPath = "XWing>Quality measurement>Measure average in rings / hulls")
public class AnalyseMeanQualityInDepthPlugin implements Command {

    @Parameter
    private ImagePlus imagePlus;

    @Parameter
    private ImagePlus qualityImagePlus;

    @Parameter
    private OpService ops;

    @Parameter
    private UIService ui;

    @Parameter (required = false)
    private int minDepthInPixels = 10;

    @Parameter (required = false)
    private int maxDepthInPixels = 100;

    @Parameter (required = false)
    private int depthStep = 10;

    @Parameter (type = ItemIO.OUTPUT)
    private Double[] measurements;

    @Parameter
    private boolean showResultTable = true;

    @Parameter
    private boolean showAnalysedRois = false;

    @Override
    public void run() {
        Img<FloatType> image = ImageJFunctions.convertFloat(imagePlus);
        Img<FloatType> qualityImage = ImageJFunctions.convertFloat(qualityImagePlus);

        AverageInDepthCalculator<FloatType> averageInDepthCalculator = new AverageInDepthCalculator<FloatType>(image, qualityImage, ops);
        averageInDepthCalculator.setMinDepthInPixels(minDepthInPixels);
        averageInDepthCalculator.setMaxDepthInPixels(maxDepthInPixels);
        averageInDepthCalculator.setDepthStep(depthStep);
        measurements = averageInDepthCalculator.getMeasurements();

        RandomAccessibleInterval<BoolType>[] analysedRegions = averageInDepthCalculator.getAnalysedRegions();
        Overlay overlay = imagePlus.getOverlay();
        if (overlay == null) {
            overlay = new Overlay();
        }

        ResultsTable resultsTable = ResultsTable.getResultsTable();
        for (int i = 0; i < measurements.length; i++) {
            resultsTable.incrementCounter();
            resultsTable.addValue("Min depth", minDepthInPixels + i * depthStep);
            resultsTable.addValue("Max depth", minDepthInPixels + (i + 1) * depthStep);
            resultsTable.addValue("Average", measurements[i]);

            overlay.add(new RegionToRoiConverter(analysedRegions[i]).getRoi());
        }
        if (showResultTable) {
            resultsTable.show("Results");
        }

        if ( showAnalysedRois ) {
            imagePlus.setOverlay(overlay);
            imagePlus.show();
        }
    }

    public static void main(String... args) throws IOException
    {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus input = IJ.openImage("src/main/resources/edf_sample.tif");

        ij.ui().show(input);

        Object result = ij.command().run(AnalyseMeanQualityInDepthPlugin.class, true, new Object[]{"imagePlus", input} );
        System.out.println(result);
    }

}
