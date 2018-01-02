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
@Plugin(type = Command.class, menuPath = "XWing>Internal (experimental)>Shannon Entropy 2D slice by slice")
public class ShannonEntropy2DSliceBySlice<T extends RealType<T>> implements
        Command
{
    @Parameter
    private ImagePlus imp;

    @Parameter private int radius = 3;

    private int numberOfSamples = 256;

    @Parameter private UIService uiService;

    @Parameter(type = ItemIO.OUTPUT)
    Img<FloatType> stdDevImg;

    @Override public void run()
    {
        Img<FloatType> image = ImageJFunctions.convertFloat(imp);
        stdDevImg = process(image, radius, numberOfSamples);
        uiService.show("stddev", stdDevImg);

        System.out.println("Bye.");
    }

    public static <T extends RealType<T>> Img<FloatType> process(RandomAccessibleInterval<T> image, int radius, int numberOfSamples) {
        if(image.numDimensions() == 2) {

            Img<FloatType> stdDevImg =
                    PlanarImgs.floats(new long[] { image.dimension(0),
                            image.dimension(1) });


            RandomAccess<FloatType> raStdDev = stdDevImg.randomAccess();

            long[] position = new long[2];

            process2D(radius, numberOfSamples, raStdDev, position, image);
            return stdDevImg;
        }
        else {
            System.out.println("Size " + image.dimension(0) + "/" + image.dimension(1) + "/" + image.dimension(2));


            Img<FloatType> stdDevImg =
                    PlanarImgs.floats(new long[] { image.dimension(0),
                            image.dimension(1),
                            image.dimension(2) });

            process3D(image, radius, stdDevImg, numberOfSamples);
            return stdDevImg;
        }
    }

    private static <T extends RealType<T>> void process3D(RandomAccessibleInterval<T> image, int radius, Img<FloatType> stdDevImg, int numberOfSamples) {

        RandomAccess<FloatType> raStdDev = stdDevImg.randomAccess();
        int numberOfSlices = (int) image.dimension(2);


        long[] position = new long[3];
        for (int z = 0; z < numberOfSlices; z++)
        {
            System.out.println("Slice " + z);

            position[2] = z;
            RandomAccessibleInterval<T>
                    slice = Views.hyperSlice(image, 2, z);

            process2D(radius, numberOfSamples, raStdDev, position, slice);
        }
    }

    private static <T extends RealType<T>> void process2D(int radius, int numberOfSamples, RandomAccess<FloatType> raStdDev, long[] position, RandomAccessibleInterval<T> slice) {
        RandomAccessibleInterval<T> source = Views.expandBorder( slice, new long[] {-radius, -radius} );
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        Cursor<T> cursor = Views.iterable(slice).cursor();

        while ( cursor.hasNext()  ) {
            double val = cursor.next().getRealDouble();
            if (minValue > val) {
                minValue = val;
            }
            if (maxValue < val) {
                maxValue = val;
            }

        }
        double range = maxValue - minValue;

        final RectangleShape shape = new RectangleShape(radius, false );
        int count = (int)Math.pow(radius * 2 + 1, 2);

        long[] histogram = new long[numberOfSamples];
        for ( final Neighborhood< T > localNeighborhood : shape.neighborhoods(source ) )
        {

            for (int i = 0; i < numberOfSamples; i++) {
                histogram[i] = 0;
            }

            for ( final T value : localNeighborhood ) {
                double val = value.getRealDouble();
                histogram[(int)((val - minValue) / range * (numberOfSamples - 1))]++;
            }

            double entropy = 0;
            for (int i = 0; i < numberOfSamples; i++) {
                if (histogram[i] > 0) {
                    double frequency = (double)histogram[i] / count;
                    entropy -= frequency * Math.log(frequency);
                }
            }

            position[0] = localNeighborhood.getIntPosition(0);
            position[1] = localNeighborhood.getIntPosition(1);

            raStdDev.setPosition(position);
            raStdDev.get().setReal(entropy);
        }
    }
}