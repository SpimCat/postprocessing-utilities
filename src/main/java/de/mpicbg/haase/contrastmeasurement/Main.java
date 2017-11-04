package de.mpicbg.haase.contrastmeasurement;

import de.mpicbg.haase.contrastmeasurement.plugins.EDFProjection;
import de.mpicbg.haase.contrastmeasurement.scijava.*;
import de.mpicbg.haase.contrastmeasurement.scijava.statistics.Average;
import de.mpicbg.haase.contrastmeasurement.scijava.statistics.StandardDeviation;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorSingleBoundary;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Random;

public class Main
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

    Object[] parameters = new Object[]{
        "input", input,
        "focusedRegionRadius", focusedRegionRadius,
        "samplingFactor", downScalingFactor
    };

    ij.command().run(EDFProjection.class, true, parameters);
  }


}
