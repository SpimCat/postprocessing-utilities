package de.mpicbg.haase.xwingedfreconstruction.scijava.statistics;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

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
