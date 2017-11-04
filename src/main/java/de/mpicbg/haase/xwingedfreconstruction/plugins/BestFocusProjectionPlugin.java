package de.mpicbg.haase.xwingedfreconstruction.plugins;

import de.mpicbg.haase.xwingedfreconstruction.scijava.StandardDeviationPerSliceMeasurement;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */

@Plugin(type = Command.class, menuPath = "XWing>Internal>Best focused slice")
public class BestFocusProjectionPlugin<T extends RealType<T>> implements Command
{
  @Parameter private Img<T> input;
  @Parameter private UIService uiService;

  @Override public void run()
  {
    StandardDeviationPerSliceMeasurement standardDeviationPerSliceMeasurement = new StandardDeviationPerSliceMeasurement(input);

    double[] stdDevs = standardDeviationPerSliceMeasurement.getStandardDeviationPerSlice();

    double max = stdDevs[0];
    long argMax = 0;

    int count = 0;
    for (double stdDev : stdDevs) {
      if (stdDev > max ) {
        max = stdDev;
        argMax = count;
      }
      count++;
    }

    RandomAccessibleInterval<T>
        slice = Views.hyperSlice(input, 2, argMax);

    uiService.show("Best focus slice from " + input.dimension(2), slice);
  }
}
