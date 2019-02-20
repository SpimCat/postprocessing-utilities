package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.plotting;

import autopilot.utils.math.Minimum;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.codehaus.groovy.control.io.ReaderSource;
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
    private static String column = "";

    private static String plotTitle = "";
    private static String plotXTitle = "X";
    private static String plotYTitle = "Y";

    private static int width = 400;
    private static int height = 300;


    @Override
    public void run() {

        ResultsTable table = ResultsTable.getResultsTable();

        GenericDialogPlus gd = new GenericDialogPlus("Plot table time point by time point in a folder");
        gd.addDirectoryField("Input directory", outputFolder);
        gd.addChoice("Column to plot:", table.getHeadings(), table.getHeadings()[0]);
        gd.addStringField("Plot title", plotTitle);
        gd.addStringField("X label", plotXTitle);
        gd.addStringField("Y label", plotYTitle);
        gd.addNumericField("Plot width", width, 0);
        gd.addNumericField("Plot height", height, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        outputFolder = gd.getNextString();
        column = gd.getNextChoice();

        plotTitle = gd.getNextString();
        plotXTitle = gd.getNextString();
        plotYTitle = gd.getNextString();

        width = (int)gd.getNextNumber();
        height = (int)gd.getNextNumber();

        int columnIndex = table.getColumnIndex(column);

        double[] allValues = table.getColumnAsDoubles(columnIndex);

        double minValue = new Min().evaluate(allValues);
        double maxValue = new Max().evaluate(allValues);

        double minX = 0;
        double maxX = allValues.length - 1;

        double[] allXAxis = new double[allValues.length];
        for (int i = 0; i < allXAxis.length; i++ ) {
            allXAxis[i] = i;
        }

        for (int i = 0; i < allXAxis.length; i++) {
            double[] values = new double[i+1];
            double[] xAxis = new double[i+1];

            System.arraycopy(allValues, 0, values, 0, values.length);
            System.arraycopy(allXAxis, 0, xAxis, 0, values.length);

            Plot plot = new Plot(plotTitle, plotXTitle, plotYTitle);
            plot.add("line", xAxis, values);
            plot.setLimits(minX, maxX, minValue, maxValue);
            plot.setSize(width, height);
            //plot.show();

            String timepoint = "000000" + i;
            timepoint = timepoint.substring(timepoint.length() - 6, timepoint.length());
            IJ.saveAs(plot.getImagePlus(), "tif", outputFolder + timepoint + ".tif");
        }
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
