package de.mpicbg.haase.contrastmeasurement.scijava;

import clearcl.*;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import com.drew.imaging.ImageProcessingException;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clearcl.utilities.ClearCLImageImgConverter;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;

@Plugin(type = Command.class)
public class StandardDeviationPerSliceMeasurement<T extends RealType<T>> implements Command
{
  private ClearCLContext mContext;

  @Parameter private Img<UnsignedShortType> image;

  @Parameter private UIService uiService;

  @Override public void run()
  {
    System.out.println("Size " + image.dimension(2));
    int numberOfSlices = (int)image.dimension(2);
    double[] standardDeviationPerSlice = new double[numberOfSlices];



    for (int z = 0; z < numberOfSlices; z++)
    {
      RandomAccessibleInterval<UnsignedShortType>
          slice = Views.hyperSlice(image, 2, z);

      Cursor<UnsignedShortType> cursor = Views.iterable(slice).localizingCursor();

      double sum = 0;
      long count = 0;
      while (cursor.hasNext()) {
        sum += cursor.next().get();
        count++;
      }
      double mean = sum / count;

      sum = 0;
      cursor.reset();
      while (cursor.hasNext()) {
        sum += Math.pow(cursor.next().get() - mean, 2);
      }
      double stdDev = sum / (count - 1);
      System.out.println("z: " + z + " stddev:" + stdDev);
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
