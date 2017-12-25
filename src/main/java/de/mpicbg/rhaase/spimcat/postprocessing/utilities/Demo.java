package de.mpicbg.rhaase.spimcat.postprocessing.utilities;

import de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.plugins.internal.ArgMaxProjectionPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.plugins.edf.BestFocusProjectionPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.utilities.scijava.plugins.edf.MaxStandardDeviationProjectionPlugin;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class Demo
{
  final static ImageJ ij = new ImageJ();

  final static double downScalingFactor = 0.125;
  final static int focusedRegionRadius = 5;


  public static void main(final String... args) throws Exception
  {
    ij.ui().showUI();
    demo("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10.tif");
    //demo("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet-twisted.tif");
  }

  private static void demo(String filename) throws Exception
  {
    Dataset dataset = (Dataset)ij.io().open(filename);
    ij.ui().show(dataset);

    Img<UnsignedShortType> input = (Img)dataset;



    Object[] parameters1 = new Object[]{
        "input", input
    };
    ij.command().run(BestFocusProjectionPlugin.class, true, parameters1);


    Object[] parameters2 = new Object[]{
        "input", input,
        "showMaxProjection", true,
        "showArgMaxProjection", false
    };
    ij.command().run(ArgMaxProjectionPlugin.class, true, parameters2);


    Object[] parameters3 = new Object[]{
        "input", input,
        "focusedRegionRadius", focusedRegionRadius,
        "samplingFactor", downScalingFactor
    };
    ij.command().run(MaxStandardDeviationProjectionPlugin.class, true, parameters3);
  }


}
