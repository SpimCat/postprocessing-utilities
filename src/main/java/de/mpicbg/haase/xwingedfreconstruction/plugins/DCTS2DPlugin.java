package de.mpicbg.haase.xwingedfreconstruction.plugins;

import de.mpicbg.haase.xwingedfreconstruction.scijava.DCTS2DImglib2;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.Arrays;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
@Plugin(type = Command.class, menuPath = "XWing>Internal>DCTS2D")
public class DCTS2DPlugin<T extends RealType<T>> implements Command
{
  @Parameter private Img<T> input;

  @Parameter private UIService uiService;


  @Override public void run()
  {
    DCTS2DImglib2 dcts2DImglib2 = new DCTS2DImglib2(input);
    System.out.println("DCTS2D: " + Arrays.toString(dcts2DImglib2.getDcts2d()));
  }
}
