package de.mpicbg.haase.xwingedfreconstruction.scijava;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
@Plugin(type = Command.class, menuPath = "XWing>Internal>Standard deviation 2D slice by slice")
public class StandardDeviationPerPixelMeasurement<T extends RealType<T>> implements
                                                                         Command
{
  @Parameter private RandomAccessibleInterval<T> image;

  @Parameter private int radius = 3;

  @Parameter private UIService uiService;

  @Parameter(type = ItemIO.OUTPUT) Img<FloatType> stdDevImg;

  @Override public void run()
  {
    stdDevImg = process(image, radius);
    uiService.show("stddev", stdDevImg);
    //ImageJFunctions.show(localEntropyImg);

    System.out.println("Bye.");
  }

  public static <T extends RealType<T>> Img<FloatType> process(RandomAccessibleInterval<T> image, int radius) {
    System.out.println("Size " + image.dimension(0) + "/" + image.dimension(1) + "/" + image.dimension(2));
    int numberOfSlices = (int) image.dimension(2);

    Img<FloatType> stdDevImg =
        PlanarImgs.floats(new long[] { image.dimension(0),
                                       image.dimension(1),
                                       image.dimension(2) });


    RandomAccess<FloatType> raStdDev = stdDevImg.randomAccess();
    long[] position = new long[3];
    for (int z = 0; z < numberOfSlices; z++)
    {
      System.out.println("Slice " + z);

      RandomAccessibleInterval<T>
          slice = Views.hyperSlice(image, 2, z);

      RandomAccessibleInterval<T> source = Views.expandBorder( slice, new long[] {-radius, -radius} );

      final RectangleShape shape = new RectangleShape(radius, false );

      position[2] = z;
      for ( final Neighborhood< T > localNeighborhood : shape.neighborhoods(source ) )
      {
        double sum = 0;
        long count = 0;
        for ( final T value : localNeighborhood )
        {
          sum += value.getRealDouble();
          count++;
        }
        double mean = sum / count;

        sum = 0;
        for ( final T value : localNeighborhood ) {
          sum += Math.pow(value.getRealDouble() - mean, 2);
        }
        double stdDev = sum / (count - 1);
        position[0] = localNeighborhood.getIntPosition(0);
        position[1] = localNeighborhood.getIntPosition(1);

        raStdDev.setPosition(position);
        raStdDev.get().setReal(stdDev);
      }
    }
    return stdDevImg;
  }
}
