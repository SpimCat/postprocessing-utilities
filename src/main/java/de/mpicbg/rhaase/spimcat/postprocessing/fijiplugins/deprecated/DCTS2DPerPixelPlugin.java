package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.deprecated;

import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.jtransforms.dct.DoubleDCT_2D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * November 2017
 */
@Deprecated
@Plugin(type = Command.class, menuPath = "SpimCat>Internal (experimental)>Local DCTS2D Map slice by slice")
public class DCTS2DPerPixelPlugin<T extends RealType<T>> implements
        Command {
    @Parameter
    private ImagePlus imp;

    @Parameter
    private int radius = 10;

    @Parameter
    private UIService uiService;

    @Parameter(type = ItemIO.OUTPUT)
    Img<FloatType> output;

    @Override
    public void run() {
        Img<T> real = ImageJFunctions.wrapReal(imp);

        output = process(real, radius);

        System.out.println("Bye.");
    }

    public static <T extends RealType<T>> Img<FloatType> process(
            RandomAccessibleInterval<T> image,
            int radius) {

        if (image.numDimensions() == 2) {


            return process2D(image, radius);
        } else {

            return process3D(image, radius);
        }
    }


    public static <T extends RealType<T>> Img<FloatType> process2D(RandomAccessibleInterval<T> image, int radius) {
        System.out.println("Size "
                + image.dimension(0)
                + "/"
                + image.dimension(1)
        );

        Img<FloatType> stdDevImg =
                PlanarImgs.floats(new long[]{image.dimension(0),
                        image.dimension(1)});

        int localWidth = 2 * radius + 1;
        int localHeight = 2 * radius + 1;
        final DoubleDCT_2D
                lDCTForWidthAndHeight =
                new DoubleDCT_2D(localWidth, localHeight);

        int pPSFSupportRadius = 3;

        double[] array = new double[(int) (localWidth * localHeight)];

        RandomAccess<FloatType> raStdDev = stdDevImg.randomAccess();
        long[] position = new long[2];

        RandomAccessibleInterval<T> slice = image;

        processNeighborhoods(radius,
                localWidth,
                lDCTForWidthAndHeight,
                pPSFSupportRadius,
                array,
                raStdDev,
                position,
                slice);
        System.out.println("Bye");

        return stdDevImg;
    }

    private static <T extends RealType<T>> void processNeighborhoods(int radius,
                                                                     int localWidth,
                                                                     DoubleDCT_2D lDCTForWidthAndHeight,
                                                                     int pPSFSupportRadius,
                                                                     double[] array,
                                                                     RandomAccess<FloatType> raStdDev,
                                                                     long[] position,
                                                                     RandomAccessibleInterval<T> slice) {
        RandomAccessibleInterval<T> source = Views.expandBorder(slice, new long[]{-radius, -radius});

        final RectangleShape shape = new RectangleShape(radius, false);

        for (final Neighborhood<T> localNeighborhood : shape.neighborhoods(source)) {
            Cursor<T> cursor = localNeighborhood.localizingCursor();
            double localEntropy = DCTS22PerSlice.entropyOfSlice(cursor, array, lDCTForWidthAndHeight, localWidth, localWidth, pPSFSupportRadius);

            position[0] = localNeighborhood.getIntPosition(0);
            position[1] = localNeighborhood.getIntPosition(1);
            raStdDev.setPosition(position);
            raStdDev.get().setReal(localEntropy * 10000000);
        }
    }

    public static <T extends RealType<T>> Img<FloatType> process3D(RandomAccessibleInterval<T> image, int radius) {
        System.out.println("Size "
                + image.dimension(0)
                + "/"
                + image.dimension(1)
                + "/"
                + image.dimension(2));
        int numberOfSlices = (int) image.dimension(2);

        Img<FloatType> stdDevImg =
                PlanarImgs.floats(new long[]{image.dimension(0),
                        image.dimension(1),
                        image.dimension(2)});

        int localWidth = 2 * radius + 1;
        int localHeight = 2 * radius + 1;
        final DoubleDCT_2D
                lDCTForWidthAndHeight =
                new DoubleDCT_2D(localWidth, localHeight);

        int pPSFSupportRadius = 3;

        double[] array = new double[(int) (localWidth * localHeight)];

        RandomAccess<FloatType> raStdDev = stdDevImg.randomAccess();
        long[] position = new long[3];
        for (int z = 0; z < numberOfSlices; z++) {
            position[2] = z;
            System.out.println("Slice " + z);

            RandomAccessibleInterval<T>
                    slice = Views.hyperSlice(image, 2, z);


            processNeighborhoods(radius,
                    localWidth,
                    lDCTForWidthAndHeight,
                    pPSFSupportRadius,
                    array,
                    raStdDev,
                    position,
                    slice);

      /*
      RandomAccessibleInterval<T> source = Views.expandBorder( slice, new long[] {-radius, -radius} );

      final RectangleShape shape = new RectangleShape(radius, false );

      for ( final Neighborhood< T > localNeighborhood : shape.neighborhoods(source ) )
      {
        Cursor<T> cursor = localNeighborhood.localizingCursor();
        double localEntropy = DCTS22PerSlice.entropyOfSlice(cursor, array, lDCTForWidthAndHeight, localWidth, localWidth, pPSFSupportRadius );

        position[0] = localNeighborhood.getIntPosition(0);
        position[1] = localNeighborhood.getIntPosition(1);
        raStdDev.setPosition(position);
        raStdDev.get().setReal(localEntropy * 10000000);
      }
      */
        }
        System.out.println("Bye");
        return stdDevImg;
    }
}
