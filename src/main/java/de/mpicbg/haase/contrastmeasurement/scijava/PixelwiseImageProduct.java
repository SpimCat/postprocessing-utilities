package de.mpicbg.haase.contrastmeasurement.scijava;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.operators.Mul;
import net.imglib2.view.Views;

/**
 *
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class PixelwiseImageProduct<T extends RealType<T>, S extends RealType<S>>
{
  private RandomAccessibleInterval<T> img1;
  private RandomAccessibleInterval<S> img2;

  private RandomAccessibleInterval<FloatType> result = null;

  public PixelwiseImageProduct(RandomAccessibleInterval<T> img1, RandomAccessibleInterval<S> img2) {
    assert img1.numDimensions() == img2.numDimensions();
    this.img1 = img1;

    long[] size = new long[img1.numDimensions()];
    for (int d = 0; d < img1.numDimensions(); d++) {
      size[d] = img1.dimension(d);
    }

    this.img2 = Views.expandBorder(img2, size);
  }

  private synchronized void process() {
    if (result != null) {
      return;
    }

    Cursor<T> cursor = Views.iterable(img1).localizingCursor();
    RandomAccess<S> readRA = img2.randomAccess();

    long[] size = new long[img1.numDimensions()];
    for (int d = 0; d < img1.numDimensions(); d++) {
      size[d] = img1.dimension(d);
    }

    result = ArrayImgs.floats(size);
    RandomAccess<FloatType> writeRA = result.randomAccess();


    while (cursor.hasNext()) {
      T value = cursor.next();

      readRA.setPosition(cursor);
      writeRA.setPosition(cursor);

      writeRA.get().setReal(readRA.get().getRealDouble() * value.getRealDouble());
    }
  }

  public RandomAccessibleInterval<FloatType> getProduct() {
    process();
    return result;
  }

}
