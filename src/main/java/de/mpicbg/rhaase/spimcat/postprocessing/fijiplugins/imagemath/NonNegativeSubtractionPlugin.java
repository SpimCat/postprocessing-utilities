package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imagemath;

import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */
@Plugin(type = Command.class, menuPath = "SpimCat>Internal (experimental)>Non-negative subtraction")
public class NonNegativeSubtractionPlugin<T extends RealType<T>> implements Command{

    @Parameter
    private Img<T> img1;

    @Parameter
    private Img<T> img2;

    @Parameter
    private OpService opService;

    @Parameter
    private UIService uiService;


    @Override
    public void run() {
        NonNegativeSubtraction<T> nonNegativeSubtraction = new NonNegativeSubtraction<T>((IterableInterval<T>) img1, img2, opService);
        uiService.show("Non negative subtraction result", nonNegativeSubtraction.getResult());
    }
}
