package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.imagemath;

import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * January 2018
 */
public class NonNegativeSubtraction<T extends RealType<T>> {
    IterableInterval<T> img1;
    RandomAccessibleInterval<T> img2;
    private Img<T> resultImg = null;

    private OpService ops;

    public NonNegativeSubtraction(IterableInterval<T> img1, RandomAccessibleInterval<T> img2, OpService ops) {
        this.img1 = img1;
        this.img2 = img2;
        this.ops = ops;
    }

    public NonNegativeSubtraction(RandomAccessibleInterval<T> img1, RandomAccessibleInterval<T> img2, OpService ops) {
        this.img1 = Views.iterable(img1);
        this.img2 = img2;
        this.ops = ops;
    }

    private synchronized void process() {
        if (resultImg != null) {
            return;
        }

        Dimensions dimensions = img1;


/*        long[] dimensions = new long[img1.numDimensions()];
        for (int d = 0; d < dimensions.length; d ++) {
            dimensions[d] = img1.numDimensions();
        }*/

        ImgFactory<T> factory = (ImgFactory)ops.run(net.imagej.ops.Ops.Create.ImgFactory.class, new Object[]{dimensions});
                //ops.create().imgFactory(dimensions);
        Cursor<T> cursor = img1.localizingCursor();
        T t = cursor.next();
        resultImg = (Img)ops.run(net.imagej.ops.Ops.Create.Img.class, new Object[]{dimensions, t, factory});
        //ops.create().img(dimensions, t, factory);

        cursor.reset();

        RandomAccess<T> source2 = img2.randomAccess();
        RandomAccess<T> target = resultImg.randomAccess();

        long[] position = new long[img1.numDimensions()];
        while(cursor.hasNext()) {
            T value = cursor.next();

            cursor.localize(position);

            source2.setPosition(position);
            target.setPosition(position);

            target.get().setReal(value.getRealDouble() - source2.get().getRealDouble());
        }

    }

    public Img<T> getResult() {
        process();
        return resultImg;
    }


}
