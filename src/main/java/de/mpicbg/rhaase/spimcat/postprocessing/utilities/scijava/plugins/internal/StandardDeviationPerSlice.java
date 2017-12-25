package de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.plugins.internal;

import de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.statistics.StandardDeviation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class StandardDeviationPerSlice<T extends RealType<T>>
{
  private RandomAccessibleInterval<T> image;
  double[] standardDeviationPerSlice = null;

  public StandardDeviationPerSlice(RandomAccessibleInterval<T> image) {
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

      double stdDev = new StandardDeviation<T>(Views.iterable(slice)).getStandardDevation();
      standardDeviationPerSlice[z] = stdDev;
    }
    System.out.println("Shown images");
  }

  public double[] getStandardDeviationPerSlice() {
    process();

    return standardDeviationPerSlice;

  }
}
