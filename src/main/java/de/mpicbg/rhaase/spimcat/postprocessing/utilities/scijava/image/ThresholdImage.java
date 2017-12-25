package de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.image;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class ThresholdImage<T extends RealType<T>>
{
  private RandomAccessibleInterval<T> image;
  private double threshold;

  private RandomAccessibleInterval<BitType> result = null;

  public ThresholdImage(RandomAccessibleInterval<T> image, double threshold) {
    this.image = image;
    this.threshold = threshold;
  }

  private synchronized void process() {
    if (result != null) {
      return;
    }

    Cursor<T> cursor = Views.iterable(image).localizingCursor();

    long[] size = new long[image.numDimensions()];
    for (int d = 0; d < image.numDimensions(); d++) {
      size[d] = image.dimension(d);
    }

    result = ArrayImgs.bits(size);
    RandomAccess<BitType> writeRA = result.randomAccess();


    while (cursor.hasNext()) {
      T value = cursor.next();

      writeRA.setPosition(cursor);

      writeRA.get().set(value.getRealDouble()> threshold);
    }
  }

  public RandomAccessibleInterval<BitType> getBinary() {
    process();
    return result;
  }
}
