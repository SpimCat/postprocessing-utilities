package de.mpicbg.haase.xwingedfreconstruction;

import de.mpicbg.haase.xwingedfreconstruction.plugins.ArgMaxProjectionPlugin;
import de.mpicbg.haase.xwingedfreconstruction.plugins.BestFocusProjectionPlugin;
import de.mpicbg.haase.xwingedfreconstruction.plugins.EDFReconstructionPlugin;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.plugin.Parameter;

public class Demo
{
  final static ImageJ ij = new ImageJ();

  final static double downScalingFactor = 0.125;
  final static int focusedRegionRadius = 5;


  public static void main(final String... args) throws Exception
  {
    ij.ui().showUI();

    Dataset dataset = (Dataset)ij.io().open("C:\\structure\\data\\xwing\\2017-11-01-EDF\\EDF5_focus_going_through_lightsheet_10.tif");
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
    ij.command().run(EDFReconstructionPlugin.class, true, parameters3);
  }


}
