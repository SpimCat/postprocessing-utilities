package de.mpicbg.haase.contrastmeasurement;

import de.mpicbg.haase.contrastmeasurement.scijava.StandardDeviationPerPixelMeasurement;
import de.mpicbg.haase.contrastmeasurement.scijava.StandardDeviationPerSliceMeasurement;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;

public class Main
{
  public static void main(final String... args) throws Exception
  {
    // Run ImageJ
    final ImageJ ij = new ImageJ();
    ij.ui().showUI();
    Object object = ij.io().open("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet.tif");
    Dataset dataset = (Dataset)object;
    ij.ui().show(dataset);

    System.out.println(dataset.getClass().getCanonicalName());

    // Create test data
    int size = 256;

    Img img = dataset;

    Object[]
        imglibParameters =
        new Object[] { "radius", 5,
                       "image", img};

    ij.op().filter().

    ij.ui().show(img);

    //ij.command().run(StandardDeviationPerSliceMeasurement.class, true, imglibParameters);
    ij.command().run(StandardDeviationPerPixelMeasurement.class, true, imglibParameters);

    System.out.println("Bye!");

  }
}
