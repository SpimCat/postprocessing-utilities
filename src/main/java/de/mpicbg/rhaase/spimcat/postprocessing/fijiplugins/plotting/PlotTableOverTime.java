package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.plotting;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

/**
 * PlotTableOverTime
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 02 2019
 */

@Plugin(type = Command.class, menuPath = "SpimCat>Presentation>Plot table time point by time point in a folder")
public class PlotTableOverTime implements Command {

    private static String outputFolder = "C:/structure/temp/plots/";
    private static String columnX = "";
    private static String columnY = "";

    public static String plotTitle = "";
    public static String plotXTitle = "X";
    public static String plotYTitle = "Y";

    public static int width = 400;
    public static int height = 300;


    @Override
    public void run() {

        ResultsTable table = ResultsTable.getResultsTable();


        GenericDialogPlus gd = new GenericDialogPlus("Plot table time point by time point in a folder");
        gd.addDirectoryField("Input directory", outputFolder);
        gd.addChoice("X_Column_to_plot:", table.getHeadings(), columnX);
        gd.addChoice("Y_Column_to_plot:", table.getHeadings(), columnY);
        gd.addStringField("Plot_title", plotTitle);
        gd.addStringField("X_label", plotXTitle);
        gd.addStringField("Y_label", plotYTitle);
        gd.addNumericField("Plot_width", width, 0);
        gd.addNumericField("Plot_height", height, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        outputFolder = gd.getNextString();
        columnX = gd.getNextChoice();
        columnY = gd.getNextChoice();

        plotTitle = gd.getNextString();
        plotXTitle = gd.getNextString();
        plotYTitle = gd.getNextString();

        width = (int)gd.getNextNumber();
        height = (int)gd.getNextNumber();

        int columnXIndex = table.getColumnIndex(columnX);
        int columnYIndex = table.getColumnIndex(columnY);

        double[] allValues = table.getColumnAsDoubles(columnYIndex);
        double[] allXAxis = table.getColumnAsDoubles(columnXIndex);

        double minValue = new Min().evaluate(allValues);
        double maxValue = new Max().evaluate(allValues);

        double minX = new Min().evaluate(allXAxis);
        double maxX = new Max().evaluate(allXAxis);

        for (int i = 0; i < allXAxis.length; i++) {
            Plot plot = getPlot(allValues, allXAxis, minValue, maxValue, minX, maxX, i);

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());
            IJ.saveAs(plot.getImagePlus(), "tif", outputFolder + timepoint + ".tif");

            IJ.showProgress(i, allXAxis.length - 1);
        }
    }

    public static Plot getPlot(double[] allValues, double[] allXAxis, double minValue, double maxValue, double minX, double maxX, int i) {
        double[] values = new double[i+1];
        double[] xAxis = new double[i+1];

        System.arraycopy(allValues, 0, values, 0, values.length);
        System.arraycopy(allXAxis, 0, xAxis, 0, values.length);

        Plot plot = new Plot(plotTitle, plotXTitle, plotYTitle);
        plot.add("line", xAxis, values);
        plot.setLimits(minX, maxX, minValue, maxValue);
        plot.setSize(width, height);
        //plot.show();
        return plot;
    }

    public static void main(String... args) {

        new ImageJ();

        ResultsTable table = ResultsTable.getResultsTable();

        table.incrementCounter();
        table.addValue("A", 10);

        table.incrementCounter();
        table.addValue("A", 11);

        table.incrementCounter();
        table.addValue("A", 12);

        table.incrementCounter();
        table.addValue("A", 5);

        table.incrementCounter();
        table.addValue("A", 6);

        table.incrementCounter();
        table.addValue("A", 7);

        table.show("Results");

        new PlotTableOverTime().run();
    }
}
