package de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.plugins.internal;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class ZMapProjection<T extends RealType<T>, I extends IntegerType<I>>
{
  private RandomAccessibleInterval<T> input;
  private RandomAccessibleInterval<I> zMap;

  // TODO: Ask somebody (Tobi?) how to make this type equal T
  private Img<UnsignedShortType> projection = null;

  public ZMapProjection(RandomAccessibleInterval<T> input, RandomAccessibleInterval<I> zMap) {
    this.input = input;
    this.zMap = zMap;
  }

  private synchronized void process() {
    if (projection != null) {
      return;
    }

    long[] size = new long[] {input.dimension(0), input.dimension(1)};

    projection = ArrayImgs.unsignedShorts(size);

    Cursor<I> cursor = Views.iterable(zMap).localizingCursor();
    RandomAccess<T> raInput = input.randomAccess();
    RandomAccess<UnsignedShortType> raProjection = projection.randomAccess();

    long[] position3d = new long[3];

    while (cursor.hasNext()) {

      position3d[2] = cursor.next().getInteger();
      position3d[0] = cursor.getIntPosition(0);
      position3d[1] = cursor.getIntPosition(1);

      raInput.setPosition(position3d);

      raProjection.setPosition(cursor);

      raProjection.get().set((int)raInput.get().getRealDouble());
    }
  }

  public Img<UnsignedShortType> getProjection() {
    process();
    return projection;
  }

}
