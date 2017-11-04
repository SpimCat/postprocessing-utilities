package de.mpicbg.haase.contrastmeasurement;

import de.mpicbg.haase.contrastmeasurement.scijava.StandardDeviationPerPixelMeasurement;
import de.mpicbg.haase.contrastmeasurement.scijava.StandardDeviationPerSliceMeasurement;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;

public class Main
{
  public static void main(final String... args) throws Exception
  {
    // Run ImageJ
    final ImageJ ij = new ImageJ();
    ij.ui().showUI();
    Object object = ij.io().open("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10.tif");
    Dataset dataset = (Dataset)object;
    ij.ui().show(dataset);

    System.out.println(dataset.getClass().getCanonicalName());

    // Create test data
    int size = 256;

    Img img = dataset;


    ij.ui().show(img);

    InterpolatorFactory interpolatorFactory = new NLinearInterpolatorFactory<>();

    RandomAccessibleInterval downScaledImg = ij.op().transform().scale(img, new double[] { 0.25, 0.25, 1}, interpolatorFactory);



    Object[]
        imglibParameters =
        new Object[] { "radius", 5,
                       "image", downScaledImg};

    //ij.command().run(StandardDeviationPerSliceMeasurement.class, true, imglibParameters);
    ij.command().run(StandardDeviationPerPixelMeasurement.class, true, imglibParameters);

    System.out.println("Bye!");

  }
}
