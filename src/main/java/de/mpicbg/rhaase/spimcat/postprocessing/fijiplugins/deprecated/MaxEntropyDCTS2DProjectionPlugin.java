package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.deprecated;

import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.deprecated.DCTS2DPerPixelPlugin;
import de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection.AbstractMaxProjectionPlugin;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
@Deprecated
@Plugin(type = Command.class, menuPath = "XWing>EDF projection>Max Entropy (DCTS2D) projection")
public class MaxEntropyDCTS2DProjectionPlugin<T extends RealType<T>> extends
        AbstractMaxProjectionPlugin<T> implements Command
{
  @Parameter protected int focusedRegionRadius = 5;

  @Parameter protected double samplingFactor = 0.5;

  @Parameter private Img<T> input;

  //@Parameter private UIService uiService;

  @Parameter(type = ItemIO.OUTPUT) RandomAccessibleInterval output;

  @Override public void run()
  {
    output = project(input,
                     samplingFactor,
                     DCTS2DPerPixelPlugin.process(resampleStackSliceBySlice(input,
                                                                    samplingFactor),
                                                             focusedRegionRadius));
  }

}