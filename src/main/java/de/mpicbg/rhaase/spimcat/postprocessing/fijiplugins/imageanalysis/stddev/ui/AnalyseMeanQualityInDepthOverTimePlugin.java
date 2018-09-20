package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.stddev.ui;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.stddev.AverageInDepthCalculator;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Quality measurement>Measure average in rings / hulls over time")
public class AnalyseMeanQualityInDepthOverTimePlugin implements Command {

    @Parameter
    private ImagePlus imagePlus;

    @Parameter
    private ImagePlus qualityImagePlus;

    @Parameter
    private CommandService commandService;

    @Parameter
    private OpService ops;

    @Parameter (required = false)
    private int minDepthInPixels = 10;

    @Parameter (required = false)
    private int maxDepthInPixels = 100;

    @Parameter (required = false)
    private int depthStep = 10;

    @Parameter
    private boolean reportOnPixelCount = false;


    @Override
    public void run() {

        Double[][] measurements = new Double[imagePlus.getNSlices()][];
        Long[][] pixelCounts = new Long[imagePlus.getNSlices()][];
        for (int z = 0; z < imagePlus.getNSlices(); z++) {
            System.out.println("z: " + z);

            Img<FloatType> image = ImageJFunctions.convertFloat(new Duplicator().run(imagePlus, z + 1, z + 1));
            Img<FloatType> qualityImage = ImageJFunctions.convertFloat(new Duplicator().run(qualityImagePlus, z + 1, z + 1));

            AverageInDepthCalculator<FloatType> averageInDepthCalculator = new AverageInDepthCalculator<FloatType>(image, qualityImage, ops);
            averageInDepthCalculator.setMinDepthInPixels(minDepthInPixels);
            averageInDepthCalculator.setMaxDepthInPixels(maxDepthInPixels);
            averageInDepthCalculator.setDepthStep(depthStep);

            measurements[z] = averageInDepthCalculator.getMeasurements();
            pixelCounts[z] = averageInDepthCalculator.getPixelCounts();
        }

        ResultsTable resultsTable = ResultsTable.getResultsTable();
        for (int i = 0; i < measurements[0].length; i++) {
            resultsTable.incrementCounter();
            resultsTable.addValue("Min_depth", minDepthInPixels + i * depthStep);
            resultsTable.addValue("Max_depth", minDepthInPixels + (i + 1) * depthStep);

            for (int z = 0; z < imagePlus.getNSlices(); z++) {
                resultsTable.addValue("Average_" + z, measurements[z][i]);
            }
            if (reportOnPixelCount) {
                for (int z = 0; z < imagePlus.getNSlices(); z++) {
                    resultsTable.addValue("Pixel_count_" + z, pixelCounts[z][i]);
                }
            }
        }
        resultsTable.show("Results");



        /*
        try {
            commandService.run(AnalyseMeanQualityInDepthPlugin.class, true, new Object[]{
                    "imagePlus", imp,
                    "minDepthInPixels", minDepthInPixels,
                    "maxDepthInPixels", maxDepthInPixels,
                    "depthStep", depthStep
            }).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/



    }



    public static void main(String... args) throws IOException
    {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus input = IJ.openImage("C:/structure/temp/xy_over_time.tif");

        ij.ui().show(input);

        Object result = ij.command().run(AnalyseMeanQualityInDepthOverTimePlugin.class, true, new Object[]{"imagePlus", input} );
        System.out.println(result);
    }

}
