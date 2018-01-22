package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imageanalysis.statistics;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class Average<T extends RealType<T>>
{
  private IterableInterval<T> image;

  private long count;
  private double mean;
  private boolean resultValid = false;

  public Average(IterableInterval<T> image) {
    this.image = image;
  }

  private synchronized void process() {
    if (resultValid) {
      return;
    }
    Cursor<T> cursor = image.localizingCursor();

    double sum = 0;
    count = 0;
    while (cursor.hasNext()) {
      sum += cursor.next().getRealDouble();
      count++;
    }
    mean = sum / count;
    System.out.println("Avg is : " + mean);
    resultValid = true;
  }

  public double getAverage() {
    process();
    return mean;
  }

  public long getCount() {
    process();
    return count;
  }
}
