package de.mpicbg.haase.contrastmeasurement;

import com.sun.org.apache.xpath.internal.Arg;
import de.mpicbg.haase.contrastmeasurement.scijava.ArgMaxProjection;
import de.mpicbg.haase.contrastmeasurement.scijava.StandardDeviationPerPixelMeasurement;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;

public class Main
{
  final static ImageJ ij = new ImageJ();

  final static double xyDownScalingFactor = 0.25;

  public static void main(final String... args) throws Exception
  {
    ij.ui().showUI();

    //    Object object = ij.io().open("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10.tif");
    //    Dataset dataset = (Dataset)object;
    //    ij.ui().show(dataset);
    //
    //    System.out.println(dataset.getClass().getCanonicalName());
    //
    //    testStdDevCalculation(dataset);

    Object
        object =
        ij.io()
          .open(
              "C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10_stdDev.tif");
    Dataset dataset = (Dataset) object;
    ij.ui().show(dataset);

    ArgMaxProjection maxProjector = new ArgMaxProjection(dataset);
    ij.ui().show("max", maxProjector.getMaxProjection());
    ij.ui().show("argMax", maxProjector.getArgMaxProjection());

  }

  private static Img<FloatType> testStdDevCalculation(Img img) throws
                                                               Exception
  {

    ij.ui().show(img);

    InterpolatorFactory
        interpolatorFactory =
        new NLinearInterpolatorFactory<>();

    RandomAccessibleInterval
        downScaledImg =
        ij.op()
          .transform()
          .scale(img,
                 new double[] { xyDownScalingFactor, xyDownScalingFactor, 1 },
                 interpolatorFactory);

    int radius = 5;

    //    Object[]
    //        imglibParameters =
    //        new Object[] { "radius", radius,
    //                       "image", downScaledImg};
    //
    //    ij.command().run(StandardDeviationPerSliceMeasurement.class, true, imglibParameters);

    Img<FloatType>
        stdDevImage =
        StandardDeviationPerPixelMeasurement.process(downScaledImg,
                                                     radius);

    System.out.println("Bye!");

    return stdDevImage;
  }
}
