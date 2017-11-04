package de.mpicbg.haase.contrastmeasurement.scijava;

import com.sun.org.apache.regexp.internal.RE;
import net.imagej.ops.create.img.Imgs;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class ArgMaxProjection<T extends RealType<T>>
{
  // input
  private RandomAccessibleInterval<T> input;

  // output
  private Img<FloatType> maxProjection = null;
  private Img<UnsignedShortType> argMaxProjection = null;

  public ArgMaxProjection(RandomAccessibleInterval<T> input) {
    this.input = input;
  }

  private synchronized void process() {
    if (maxProjection != null) {
      return;
    }

    long[] size = new long[] {input.dimension(0), input.dimension(1)};

    Cursor<T> cursor = Views.iterable(input).localizingCursor();

    maxProjection = ArrayImgs.floats(size);
    argMaxProjection = ArrayImgs.unsignedShorts(size);

    RandomAccess<FloatType> raMax = maxProjection.randomAccess();
    RandomAccess<UnsignedShortType> raArgMax = argMaxProjection.randomAccess();

    long[] position = new long[2];
    while (cursor.hasNext()) {
      double value = cursor.next().getRealDouble();

      position[0] = cursor.getIntPosition(0);
      position[1] = cursor.getIntPosition(1);

      raMax.setPosition(position);
      FloatType maxProjectionPixel = raMax.get();

      int positionZ = cursor.getIntPosition(2);

      if (maxProjectionPixel.get() < value || positionZ == 0)
      {
        raMax.setPosition(position);
        raArgMax.setPosition(position);
        maxProjectionPixel.setReal(value);
        UnsignedShortType argMaxProjectionPixel = raArgMax.get();
        argMaxProjectionPixel.set(positionZ);
      }
    }
  }

  public Img<FloatType> getMaxProjection() {
    process();
    return maxProjection;
  }

  public Img<UnsignedShortType> getArgMaxProjection() {
    process();
    return argMaxProjection;
  }
}
