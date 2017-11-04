package de.mpicbg.haase.xwingedfreconstruction.scijava;

import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.StandardDeviation;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.Arrays;

public class StandardDeviationPerSliceMeasurement<T extends RealType<T>>
{
  private RandomAccessibleInterval<T> image;
  double[] standardDeviationPerSlice = null;

  public StandardDeviationPerSliceMeasurement(RandomAccessibleInterval<T> image) {
    this.image = image;
  }

  private synchronized void process()
  {
    if (standardDeviationPerSlice != null) {
      return;
    }
    System.out.println("Size " + image.dimension(2));
    int numberOfSlices = (int)image.dimension(2);
    standardDeviationPerSlice = new double[numberOfSlices];

    for (int z = 0; z < numberOfSlices; z++)
    {
      RandomAccessibleInterval<T>
          slice = Views.hyperSlice(image, 2, z);

      double stdDev = new StandardDeviation<T>(slice).getStandardDevation();
      standardDeviationPerSlice[z] = stdDev;
    }
    System.out.println("Shown images");
  }

  public double[] getStandardDeviationPerSlice() {
    process();

    return standardDeviationPerSlice;

  }
}
