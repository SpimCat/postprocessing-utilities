package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.deprecated;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.util.Arrays;

/**
 * Useful for debugging only
 *
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
@Deprecated
//@Plugin(type = Command.class, menuPath = "SpimCat>Internal (experimental)>DCTS2D entropy slice by slice")
public class DCTS2DPerSlicePlugin<T extends RealType<T>> implements Command
{
  @Parameter private Img<T> input;

  @Parameter private UIService uiService;


  @Override public void run()
  {
    DCTS22PerSlice dcts2DImglib2 = new DCTS22PerSlice(input);
    System.out.println("DCTS2D: " + Arrays.toString(dcts2DImglib2.getDcts2d()));
  }
}
