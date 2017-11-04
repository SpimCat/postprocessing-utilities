package de.mpicbg.haase.xwingedfreconstruction.scijava;

import clearcl.*;
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

@Plugin(type = Command.class)
public class StandardDeviationPerSliceMeasurement<T extends RealType<T>> implements Command
{
  private ClearCLContext mContext;

  @Parameter private Img<T> image;

  @Parameter private UIService uiService;

  @Override public void run()
  {
    System.out.println("Size " + image.dimension(2));
    int numberOfSlices = (int)image.dimension(2);
    double[] standardDeviationPerSlice = new double[numberOfSlices];



    for (int z = 0; z < numberOfSlices; z++)
    {
      RandomAccessibleInterval<T>
          slice = Views.hyperSlice(image, 2, z);

      double stdDev = new StandardDeviation<T>(slice).getStandardDevation();
      standardDeviationPerSlice[z] = stdDev;
    }
    System.out.println("Shown images");
  }

  private void fillImage(Img<UnsignedShortType> img, int value) {
    Cursor<UnsignedShortType> cursor = img.cursor();
    while(cursor.hasNext()) {
      cursor.next().set(value);
    }
  }


}
