package de.mpicbg.haase.xwingedfreconstruction.scijava.plugins.analysis.localquality.stddev;

import de.mpicbg.haase.xwingedfreconstruction.scijava.image.operators.BinaryOperatorUtilities;
import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import java.util.HashMap;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public class RingRegionGenerator<B extends BooleanType<B>> {

    private RandomAccessibleInterval<B> binaryImg;
    private Img<FloatType> distanceMap = null;

    HashMap<Integer, RandomAccessibleInterval<BitType>> binaryImages = new HashMap<>();


    RandomAccessibleInterval<FloatType> distanceRai;

    private OpService ops;

    public RingRegionGenerator(RandomAccessibleInterval<B> binaryImg, OpService ops) {
        this.binaryImg = binaryImg;
        this.ops = ops;
    }

    public RandomAccessibleInterval<BoolType> getRing(int minDistance, int maxDistance) {
        setup();

        RandomAccessibleInterval<BitType> outerBinaryRai = getBinaryImage(minDistance);
        RandomAccessibleInterval<BitType> innerBinaryRai = getBinaryImage(maxDistance);

        RandomAccessibleInterval<BoolType> ring = BinaryOperatorUtilities.xor(outerBinaryRai, innerBinaryRai);

        return ring;
    }

    public void reset() {
        binaryImages.clear();
        distanceMap = null;
    }

    private RandomAccessibleInterval<BitType> getBinaryImage(int distance) {
        if (binaryImages.keySet().contains(distance)) {
            return binaryImages.get(distance);
        }


        IterableInterval<BitType> binaryIi = ops.threshold().apply(Views.iterable(distanceRai), new FloatType(distance));

        RandomAccessibleInterval<BitType> binaryRai = ops.convert().bit(binaryIi);
        binaryImages.put(distance, binaryRai);

        return binaryRai;
    }

    private void setup() {
        if (distanceMap != null) {
            return;
        }


        Interval interval = binaryImg;

        distanceRai = ops.image().distancetransform(binaryImg);


    }}




