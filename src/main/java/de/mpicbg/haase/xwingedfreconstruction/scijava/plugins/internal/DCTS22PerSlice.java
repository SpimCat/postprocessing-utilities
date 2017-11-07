package de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.internal;

import clearcontrol.ip.iqm.DCTS2D;
import clearcontrol.stack.OffHeapPlanarStack;
import de.mpicbg.haase.xwingedfreconstruction.scijava.statistics.StandardDeviation;
import net.haesleinhuepf.clearcl.utilities.ClearCLImageImgConverter;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.jtransforms.dct.DoubleDCT_2D;
import pl.edu.icm.jlargearrays.DoubleLargeArray;

import static java.lang.Math.sqrt;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
public class DCTS22PerSlice<T extends RealType<T>>
{
  private RandomAccessibleInterval<T> image;
  double[] dcts2d = null;

  public DCTS22PerSlice(RandomAccessibleInterval<T> image) {
    this.image = image;
  }

  private synchronized void process()
  {
    if (dcts2d != null) {
      return;
    }
    System.out.println("Size " + image.dimension(2));


    final DoubleDCT_2D lDCTForWidthAndHeight = new DoubleDCT_2D(image.dimension(0), image.dimension(1));

    int pWidth = (int)image.dimension(0);
    int pHeight = (int)image.dimension(1);
    int pPSFSupportRadius = 3;


    double[] array = new double[(int)(image.dimension(0) * image.dimension(1))];
    if (image.numDimensions() == 2) {
      dcts2d = new double[] {entropyOfSlice(Views.iterable(image).localizingCursor(), array, lDCTForWidthAndHeight, pWidth, pHeight, pPSFSupportRadius)};
    }
    else
    {

      int numberOfSlices = (int) image.dimension(2);
      dcts2d = new double[numberOfSlices];
      for (int z = 0; z < numberOfSlices; z++)
      {
        RandomAccessibleInterval<T>
            slice = Views.hyperSlice(image, 2, z);

        dcts2d[z] = entropyOfSlice(Views.iterable(slice).localizingCursor(), array, lDCTForWidthAndHeight, pWidth, pHeight, pPSFSupportRadius);
      }
    }
    System.out.println("Shown images");
  }

  public static <T extends RealType<T>> double entropyOfSlice(Cursor<T> cursor, double[] array, DoubleDCT_2D lDCTForWidthAndHeight, int pWidth, int pHeight, int pPSFSupportRadius) {

    // todo: the order of how we get the pixels is not garanteed. it may be better to go through the image with a random access

    int count = 0;
    while (cursor.hasNext())
    {
      array[count] = cursor.next().getRealDouble();
      count++;
    }

    lDCTForWidthAndHeight.forward(array, false);

    normalizeL2(array);

    final int
        lOTFSupportRadiusX =
        Math.round(pWidth / pPSFSupportRadius);
    final int
        lOTFSupportRadiusY =
        Math.round(pHeight / pPSFSupportRadius);

    final double lEntropy =
        entropyPerPixelSubTriangle(array,
                                   pWidth,
                                   pHeight,
                                   0,
                                   0,
                                   lOTFSupportRadiusX,
                                   lOTFSupportRadiusY);
    return lEntropy;
  }


  private static void normalizeL2(double[] pDoubleLargeArray)
  {
    final double lL2 = computeL2(pDoubleLargeArray);
    final double lIL2 = 1.0 / lL2;
    final long lLength = pDoubleLargeArray.length;

    for (int i = 0; i < lLength; i++)
    {
      final double lValue = pDoubleLargeArray[i];
      pDoubleLargeArray[i] = lValue * lIL2;
    }
  }

  private static double computeL2(double[] pDoubleLargeArray)
  {
    final long lLength = pDoubleLargeArray.length;

    double l2 = 0;
    for (int i = 0; i < lLength; i++)
    {
      final double lValue = pDoubleLargeArray[i];
      l2 += lValue * lValue;
    }

    return sqrt(l2);
  }


  private static final double entropyPerPixelSubTriangle(double[] pDoubleLargeArray,
                                                  final int pWidth,
                                                  final int pHeight,
                                                  final int xl,
                                                  final int yl,
                                                  final int xh,
                                                  final int yh)
  {
    double entropy = 0;
    for (int y = yl; y < yh; y++)
    {
      final int yi = y * pWidth;

      final int xend = xh - y * xh / yh;
      entropy = entropySub(pDoubleLargeArray, xl, entropy, yi, xend);
    }
    entropy = -entropy / (2 * xh * yh);

    return entropy;
  }

  private static double entropySub(double[] pDoubleLargeArray,
                            final int xl,
                            final double entropy,
                            final int yi,
                            final int xend)
  {
    double lEntropy = entropy;
    for (int x = xl; x < xend; x++)
    {
      final int i = yi + x;
      final double value = pDoubleLargeArray[i];
      if (value > 0)
      {
        lEntropy += value * Math.log(value);
      }
      else if (value < 0)
      {
        lEntropy += -value * Math.log(-value);
      }
    }
    return lEntropy;
  }

  public double[] getDcts2d() {
    process();

    return dcts2d;

  }
}
