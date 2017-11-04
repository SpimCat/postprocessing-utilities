package de.mpicbg.haase.xwingedfreconstruction.plugins;

import de.mpicbg.haase.xwingedfreconstruction.scijava.ArgMaxProjection;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */

@Plugin(type = Command.class, menuPath = "XWing>Internal>[Arg]Max projection")
public class ArgMaxProjectionPlugin<T extends RealType<T>> implements Command
{


  @Parameter private Img<T> input;

  @Parameter private UIService uiService;

  @Parameter boolean showMaxProjection = true;
  @Parameter boolean showArgMaxProjection = false;

  @Override public void run()
  {
    ArgMaxProjection<T> argMaxProjection = new ArgMaxProjection<T>(
        input);
    if (showArgMaxProjection) {
      uiService.show("ArgMax ", argMaxProjection.getArgMaxProjection());
    }
    if (showMaxProjection) {
      uiService.show("Max ", argMaxProjection.getMaxProjection());
    }
  }
}
