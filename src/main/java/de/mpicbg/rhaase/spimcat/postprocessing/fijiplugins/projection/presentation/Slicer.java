//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.mpicbg.rhaase.spimcat.postprocessing.fijiplugins.projection.presentation;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.util.Tools;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Vector;

public class Slicer implements PlugIn, TextListener, ItemListener {
    private static final String[] starts = new String[]{"Top", "Left", "Bottom", "Right"};
    private static String startAtS;
    private static boolean rotateS;
    private static boolean flipS;
    private static int sliceCountS;
    private String startAt;
    private boolean rotate;
    private boolean flip;
    private int sliceCount;
    private boolean nointerpolate;
    private double inputZSpacing;
    private double outputZSpacing;
    private int outputSlices;
    private boolean noRoi;
    private boolean rgb;
    private boolean notFloat;
    private Vector fields;
    private Vector checkboxes;
    private Label message;
    private ImagePlus imp;
    private double gx1;
    private double gy1;
    private double gx2;
    private double gy2;
    private double gLength;
    private int n;
    private double[] x;
    private double[] y;
    private int xbase;
    private int ybase;
    private double length;
    private double[] segmentLengths;
    private double[] dx;
    private double[] dy;

    public Slicer(double outputZSpacing, int outputSlices) {
        this.startAt = starts[1];
        this.sliceCount = 1;
        this.nointerpolate = false;
        this.inputZSpacing = outputZSpacing;
        this.outputZSpacing = outputZSpacing;
        this.outputSlices = outputSlices;
        System.out.println("outputslices" + outputSlices);
    }

    public void run(String arg) {
        this.imp = WindowManager.getCurrentImage();
        if (this.imp == null) {
            IJ.noImage();
        } else {
            int stackSize = this.imp.getStackSize();
            Roi roi = this.imp.getRoi();
            int roiType = roi != null ? roi.getType() : 0;
            if (stackSize < 2 && roi != null && roiType != 0) {
                IJ.error("Reslice...", "Stack required");
            } else if (roi != null && roiType != 0 && roiType != 5 && roiType != 6 && roiType != 7) {
                IJ.error("Reslice...", "Line or rectangular selection required");
            } else if (this.showDialog(this.imp)) {
                long startTime = System.currentTimeMillis();
                ImagePlus imp2 = null;
                this.rgb = this.imp.getType() == 4;
                this.notFloat = !this.rgb && this.imp.getType() != 2;
                if (this.imp.isHyperStack()) {
                    imp2 = this.resliceHyperstack(this.imp);
                } else {
                    imp2 = this.reslice(this.imp);
                }

                if (imp2 != null) {
                    ImageProcessor ip = this.imp.getProcessor();
                    double min = ip.getMin();
                    double max = ip.getMax();
                    if (!this.rgb) {
                        imp2.getProcessor().setMinAndMax(min, max);
                    }

                    imp2.show();
                    if (this.noRoi) {
                        this.imp.deleteRoi();
                    } else {
                        this.imp.draw();
                    }

                    IJ.showStatus(IJ.d2s((double)(System.currentTimeMillis() - startTime) / 1000.0D, 2) + " seconds");
                }
            }
        }
    }

    public ImagePlus reslice(ImagePlus imp) {
        Roi roi = imp.getRoi();
        int roiType = roi != null ? roi.getType() : 0;
        Calibration origCal = imp.getCalibration();
        boolean globalCalibration = false;
        if (this.nointerpolate) {
            globalCalibration = imp.getGlobalCalibration() != null;
            imp.setGlobalCalibration((Calibration)null);
            Calibration tmpCal = origCal.copy();
            tmpCal.pixelWidth = 1.0D;
            tmpCal.pixelHeight = 1.0D;
            tmpCal.pixelDepth = 1.0D;
            imp.setCalibration(tmpCal);
            this.inputZSpacing = 1.0D;
            if (roiType != 5) {
                this.outputZSpacing = 1.0D;
            }
        }

        double zSpacing = this.inputZSpacing / imp.getCalibration().pixelWidth;
        ImagePlus imp2;
        if (roi != null && roiType != 0 && roiType != 5) {
            String status = imp.getStack().isVirtual() ? "" : null;
            IJ.showStatus("Reslice...");
            ImageProcessor ip2 = this.getSlice(imp, 0.0D, 0.0D, 0.0D, 0.0D, status);
            imp2 = new ImagePlus("Reslice of " + imp.getShortTitle(), ip2);
        } else {
            imp2 = this.resliceRectOrLine(imp);
        }

        if (this.nointerpolate) {
            if (globalCalibration) {
                imp.setGlobalCalibration(origCal);
            }

            imp.setCalibration(origCal);
        }

        boolean horizontal = false;
        boolean vertical = false;
        if (roi == null || roiType == 0) {
            if (!this.startAt.equals(starts[0]) && !this.startAt.equals(starts[2])) {
                vertical = true;
            } else {
                horizontal = true;
            }
        }

        if (roi != null && roiType == 5) {
            Line l = (Line)roi;
            horizontal = l.y2 - l.y1 == 0;
            vertical = l.x2 - l.x1 == 0;
        }

        if (imp2 == null) {
            return null;
        } else {
            imp2.setCalibration(imp.getCalibration());
            Calibration cal = imp2.getCalibration();
            if (horizontal) {
                cal.pixelWidth = origCal.pixelWidth;
                cal.pixelHeight = origCal.pixelDepth / zSpacing;
                cal.pixelDepth = origCal.pixelHeight * this.outputZSpacing;
            } else if (vertical) {
                cal.pixelWidth = origCal.pixelHeight;
                cal.pixelHeight = origCal.pixelDepth / zSpacing;
                cal.pixelDepth = origCal.pixelWidth * this.outputZSpacing;
            } else if (origCal.pixelHeight == origCal.pixelWidth) {
                cal.pixelWidth = origCal.pixelWidth;
                cal.pixelHeight = origCal.pixelDepth / zSpacing;
                cal.pixelDepth = origCal.pixelWidth * this.outputZSpacing;
            } else {
                cal.pixelWidth = cal.pixelHeight = cal.pixelDepth = 1.0D;
                cal.setUnit("pixel");
            }

            if (this.rotate) {
                double tmp = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = tmp;
            }

            return imp2;
        }
    }

    ImagePlus resliceHyperstack(ImagePlus imp) {
        int channels = imp.getNChannels();
        int slices = imp.getNSlices();
        int frames = imp.getNFrames();
        if (slices == 1) {
            return this.resliceTimeLapseHyperstack(imp);
        } else {
            int c1 = imp.getChannel();
            int z1 = imp.getSlice();
            int t1 = imp.getFrame();
            int width = imp.getWidth();
            int height = imp.getHeight();
            ImagePlus imp2 = null;
            ImageStack stack2 = null;
            Roi roi = imp.getRoi();

            for(int t = 1; t <= frames; ++t) {
                for(int c = 1; c <= channels; ++c) {
                    ImageStack tmp1Stack = new ImageStack(width, height);

                    for(int z = 1; z <= slices; ++z) {
                        imp.setPositionWithoutUpdate(c, z, t);
                        tmp1Stack.addSlice((String)null, imp.getProcessor());
                    }

                    ImagePlus tmp1 = new ImagePlus("tmp", tmp1Stack);
                    tmp1.setCalibration(imp.getCalibration());
                    tmp1.setRoi(roi);
                    ImagePlus tmp2 = this.reslice(tmp1);
                    int slices2 = tmp2.getStackSize();
                    if (imp2 == null) {
                        imp2 = tmp2.createHyperStack("Reslice of " + imp.getTitle(), channels, slices2, frames, tmp2.getBitDepth());
                        stack2 = ((ImagePlus)imp2).getStack();
                    }

                    ImageStack tmp2Stack = tmp2.getStack();

                    for(int z = 1; z <= slices2; ++z) {
                        imp.setPositionWithoutUpdate(c, z, t);
                        int n2 = ((ImagePlus)imp2).getStackIndex(c, z, t);
                        stack2.setPixels(tmp2Stack.getPixels(z), n2);
                    }
                }
            }

            imp.setPosition(c1, z1, t1);
            if (channels > 1 && imp.isComposite()) {
                imp2 = new CompositeImage((ImagePlus)imp2, ((CompositeImage)imp).getMode());
                ((CompositeImage)imp2).copyLuts(imp);
            }

            return (ImagePlus)imp2;
        }
    }

    ImagePlus resliceTimeLapseHyperstack(ImagePlus imp) {
        int channels = imp.getNChannels();
        int frames = imp.getNFrames();
        int c1 = imp.getChannel();
        int t1 = imp.getFrame();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImagePlus imp2 = null;
        ImageStack stack2 = null;
        Roi roi = imp.getRoi();
        int z = 1;

        for(int c = 1; c <= channels; ++c) {
            ImageStack tmp1Stack = new ImageStack(width, height);

            for(int t = 1; t <= frames; ++t) {
                imp.setPositionWithoutUpdate(c, z, t);
                tmp1Stack.addSlice((String)null, imp.getProcessor());
            }

            ImagePlus tmp1 = new ImagePlus("tmp", tmp1Stack);
            tmp1.setCalibration(imp.getCalibration());
            tmp1.setRoi(roi);
            ImagePlus tmp2 = this.reslice(tmp1);
            int frames2 = tmp2.getStackSize();
            if (imp2 == null) {
                imp2 = tmp2.createHyperStack("Reslice of " + imp.getTitle(), channels, 1, frames2, tmp2.getBitDepth());
                stack2 = ((ImagePlus)imp2).getStack();
            }

            ImageStack tmp2Stack = tmp2.getStack();

            for(int t = 1; t <= frames2; ++t) {
                imp.setPositionWithoutUpdate(c, z, t);
                int n2 = ((ImagePlus)imp2).getStackIndex(c, z, t);
                stack2.setPixels(tmp2Stack.getPixels(z), n2);
            }
        }

        imp.setPosition(c1, 1, t1);
        if (channels > 1 && imp.isComposite()) {
            imp2 = new CompositeImage((ImagePlus)imp2, ((CompositeImage)imp).getMode());
            ((CompositeImage)imp2).copyLuts(imp);
        }

        return (ImagePlus)imp2;
    }

    boolean showDialog(ImagePlus imp) {
        Calibration cal = imp.getCalibration();
        if (cal.pixelDepth < 0.0D) {
            cal.pixelDepth = -cal.pixelDepth;
        }

        String units = cal.getUnits();
        if (cal.pixelWidth == 0.0D) {
            cal.pixelWidth = 1.0D;
        }

        this.inputZSpacing = cal.pixelDepth;
        double outputSpacing = cal.pixelDepth;
        Roi roi = imp.getRoi();
        boolean line = roi != null && roi.getType() == 5;
        if (line) {
            this.saveLineInfo(roi);
        }

        String macroOptions = Macro.getOptions();
        boolean macroRunning = macroOptions != null;
        if (macroRunning) {
            if (macroOptions.indexOf("input=") != -1) {
                macroOptions = macroOptions.replaceAll("slice=", "slice_count=");
            }

            macroOptions = macroOptions.replaceAll("slice=", "output=");
            Macro.setOptions(macroOptions);
            this.nointerpolate = false;
        } else {
            this.startAt = startAtS;
            this.rotate = rotateS;
            this.flip = flipS;
            this.sliceCount = sliceCountS;
        }

        GenericDialog gd = new GenericDialog("Reslice");
        gd.addNumericField("Output spacing (" + units + "):", outputSpacing, 3);
        if (line) {
            if (!IJ.isMacro()) {
                this.outputSlices = this.sliceCount;
            }

            gd.addNumericField("Slice_count:", (double)this.outputSlices, 0);
        } else {
            gd.addChoice("Start at:", starts, this.startAt);
        }

        gd.addCheckbox("Flip vertically", this.flip);
        gd.addCheckbox("Rotate 90 degrees", this.rotate);
        gd.addCheckbox("Avoid interpolation", this.nointerpolate);
        gd.setInsets(0, 32, 0);
        gd.addMessage("(use 1 pixel spacing)");
        gd.setInsets(15, 0, 0);
        gd.addMessage("Voxel size: " + this.d2s(cal.pixelWidth) + "x" + this.d2s(cal.pixelHeight) + "x" + this.d2s(cal.pixelDepth) + " " + cal.getUnit());
        gd.setInsets(5, 0, 0);
        gd.addMessage("Output size: " + this.getSize(cal.pixelDepth, outputSpacing, this.outputSlices) + "\t\t\t\t");
        this.fields = gd.getNumericFields();
        if (!macroRunning) {
            for(int i = 0; i < this.fields.size(); ++i) {
                ((TextField)this.fields.elementAt(i)).addTextListener(this);
            }
        }

        this.checkboxes = gd.getCheckboxes();
        if (!macroRunning) {
            ((Checkbox)this.checkboxes.elementAt(2)).addItemListener(this);
        }

        this.message = (Label)gd.getMessage();
        gd.addHelp("http://imagej.nih.gov/ij/docs/menus/image.html#reslice");
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        } else {
            this.outputZSpacing = gd.getNextNumber() / cal.pixelWidth;
            if (line) {
                this.outputSlices = (int)gd.getNextNumber();
                if (!IJ.isMacro()) {
                    this.sliceCount = this.outputSlices;
                }

                imp.setRoi(roi);
            } else {
                this.startAt = gd.getNextChoice();
            }

            this.flip = gd.getNextBoolean();
            this.rotate = gd.getNextBoolean();
            this.nointerpolate = gd.getNextBoolean();
            if (!macroRunning) {
                Prefs.avoidResliceInterpolation = this.nointerpolate;
                startAtS = this.startAt;
                rotateS = this.rotate;
                flipS = this.flip;
                sliceCountS = this.sliceCount;
            }

            return true;
        }
    }

    String d2s(double n) {
        String s;
        if (n == (double)((int)n)) {
            s = ResultsTable.d2s(n, 0);
        } else {
            s = ResultsTable.d2s(n, 2);
        }

        if (s.indexOf(".") != -1 && s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }

    void saveLineInfo(Roi roi) {
        Line line = (Line)roi;
        this.gx1 = (double)line.x1;
        this.gy1 = (double)line.y1;
        this.gx2 = (double)line.x2;
        this.gy2 = (double)line.y2;
        this.gLength = line.getRawLength();
    }

    ImagePlus resliceRectOrLine(ImagePlus imp) {
        double x1 = 0.0D;
        double y1 = 0.0D;
        double x2 = 0.0D;
        double y2 = 0.0D;
        double xInc = 0.0D;
        double yInc = 0.0D;
        this.noRoi = false;
        Roi roi = imp.getRoi();
        if (roi == null) {
            this.noRoi = true;
            imp.setRoi(0, 0, imp.getWidth(), imp.getHeight());
            roi = imp.getRoi();
        }

        if (roi.getType() == 0) {
            Rectangle r = roi.getBounds();
            if (this.startAt.equals(starts[0])) {
                x1 = (double)r.x;
                y1 = (double)r.y;
                x2 = (double)(r.x + r.width);
                y2 = (double)r.y;
                xInc = 0.0D;
                yInc = this.outputZSpacing;
                this.outputSlices = (int)((double)r.height / this.outputZSpacing);
            } else if (this.startAt.equals(starts[1])) {
                x1 = (double)r.x;
                y1 = (double)r.y;
                x2 = (double)r.x;
                y2 = (double)(r.y + r.height);
                xInc = this.outputZSpacing;
                yInc = 0.0D;
                this.outputSlices = (int)((double)r.width / this.outputZSpacing);
            } else if (this.startAt.equals(starts[2])) {
                x1 = (double)r.x;
                y1 = (double)(r.y + r.height - 1);
                x2 = (double)(r.x + r.width);
                y2 = (double)(r.y + r.height - 1);
                xInc = 0.0D;
                yInc = -this.outputZSpacing;
                this.outputSlices = (int)((double)r.height / this.outputZSpacing);
            } else if (this.startAt.equals(starts[3])) {
                x1 = (double)(r.x + r.width - 1);
                y1 = (double)r.y;
                x2 = (double)(r.x + r.width - 1);
                y2 = (double)(r.y + r.height);
                xInc = -this.outputZSpacing;
                yInc = 0.0D;
                this.outputSlices = (int)((double)r.width / this.outputZSpacing);
            }
        } else {
            if (roi.getType() != 5) {
                return null;
            }

            Line line = (Line)roi;
            x1 = (double)line.x1;
            y1 = (double)line.y1;
            x2 = (double)line.x2;
            y2 = (double)line.y2;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double nrm = Math.sqrt(dx * dx + dy * dy) / this.outputZSpacing;
            xInc = -(dy / nrm);
            yInc = dx / nrm;
        }

        if (this.outputSlices == 0) {
            IJ.error("Reslicer", "Output Z spacing (" + IJ.d2s(this.outputZSpacing, 0) + " pixels) is too large.\n" + "Is the voxel size in Image>Properties correct?.");
            return null;
        } else {
            boolean virtualStack = imp.getStack().isVirtual();
            String status = null;
            ImagePlus imp2 = null;
            ImageStack stack2 = null;
            boolean isStack = imp.getStackSize() > 1;
            IJ.resetEscape();

            for(int i = 0; i < this.outputSlices; ++i) {
                if (virtualStack) {
                    status = this.outputSlices > 1 ? i + 1 + "/" + this.outputSlices + ", " : "";
                }

                ImageProcessor ip = this.getSlice(imp, x1, y1, x2, y2, status);
                if (isStack) {
                    this.drawLine(x1, y1, x2, y2, imp);
                }

                if (stack2 == null) {
                    stack2 = this.createOutputStack(imp, ip);
                    if (stack2 == null || stack2.getSize() < this.outputSlices) {
                        return null;
                    }
                }

                stack2.setPixels(ip.getPixels(), i + 1);
                x1 += xInc;
                x2 += xInc;
                y1 += yInc;
                y2 += yInc;
                if (IJ.escapePressed()) {
                    IJ.beep();
                    imp.draw();
                    return null;
                }
            }

            return new ImagePlus("Reslice of " + imp.getShortTitle(), stack2);
        }
    }

    ImageStack createOutputStack(ImagePlus imp, ImageProcessor ip) {
        int bitDepth = imp.getBitDepth();
        int w2 = ip.getWidth();
        int h2 = ip.getHeight();
        int d2 = this.outputSlices;
        int flags = 9;
        ImagePlus imp2 = NewImage.createImage("temp", w2, h2, d2, bitDepth, flags);
        if (imp2 != null && imp2.getStackSize() == d2) {
            IJ.showStatus("Reslice... (press 'Esc' to abort)");
        }

        if (imp2 == null) {
            return null;
        } else {
            ImageStack stack2 = imp2.getStack();
            stack2.setColorModel(ip.getColorModel());
            return stack2;
        }
    }

    ImageProcessor getSlice(ImagePlus imp, double x1, double y1, double x2, double y2, String status) {
        Roi roi = imp.getRoi();
        int roiType = roi != null ? roi.getType() : 0;
        ImageStack stack = imp.getStack();
        int stackSize = stack.getSize();
        ImageProcessor ip2 = null;
        float[] line = null;
        boolean ortho = (double)((int)x1) == x1 && (double)((int)y1) == y1 && x1 == x2 || y1 == y2;

        for(int i = 0; i < stackSize; ++i) {
            ImageProcessor ip = stack.getProcessor(this.flip ? stackSize - i : i + 1);
            if (roiType != 6 && roiType != 7) {
                if (ortho) {
                    line = this.getOrthoLine(ip, (int)x1, (int)y1, (int)x2, (int)y2, line);
                } else {
                    line = this.getLine(ip, x1, y1, x2, y2, line);
                }
            } else {
                line = this.getIrregularProfile(roi, ip);
            }

            if (this.rotate) {
                if (i == 0) {
                    ip2 = ip.createProcessor(stackSize, line.length);
                }

                this.putColumn(ip2, i, 0, line, line.length);
            } else {
                if (i == 0) {
                    ip2 = ip.createProcessor(line.length, stackSize);
                }

                this.putRow(ip2, 0, i, line, line.length);
            }

            if (status != null) {
                IJ.showStatus("Slicing: " + status + i + "/" + stackSize);
            }
        }

        Calibration cal = imp.getCalibration();
        double zSpacing = this.inputZSpacing / cal.pixelWidth;
        if (zSpacing != 1.0D) {
            ip2.setInterpolate(true);
            if (this.rotate) {
                ip2 = ip2.resize((int)((double)stackSize * zSpacing), line.length);
            } else {
                ip2 = ip2.resize(line.length, (int)((double)stackSize * zSpacing));
            }
        }

        return ip2;
    }

    public void putRow(ImageProcessor ip, int x, int y, float[] data, int length) {
        int i;
        if (this.rgb) {
            for(i = 0; i < length; ++i) {
                ip.putPixel(x++, y, Float.floatToIntBits(data[i]));
            }
        } else {
            for(i = 0; i < length; ++i) {
                ip.putPixelValue(x++, y, (double)data[i]);
            }
        }

    }

    public void putColumn(ImageProcessor ip, int x, int y, float[] data, int length) {
        int i;
        if (this.rgb) {
            for(i = 0; i < length; ++i) {
                ip.putPixel(x, y++, Float.floatToIntBits(data[i]));
            }
        } else {
            for(i = 0; i < length; ++i) {
                ip.putPixelValue(x, y++, (double)data[i]);
            }
        }

    }

    float[] getIrregularProfile(Roi roi, ImageProcessor ip) {
        if (this.x == null) {
            this.doIrregularSetup(roi);
        }

        float[] values = new float[(int)this.length];
        double leftOver = 1.0D;
        double distance = 0.0D;
        double oldx = (double)this.xbase;
        double oldy = (double)this.ybase;

        for(int i = 0; i < this.n; ++i) {
            double len = this.segmentLengths[i];
            if (len != 0.0D) {
                double xinc = this.dx[i] / len;
                double yinc = this.dy[i] / len;
                double start = 1.0D - leftOver;
                double rx = (double)this.xbase + this.x[i] + start * xinc;
                double ry = (double)this.ybase + this.y[i] + start * yinc;
                double len2 = len - start;
                int n2 = (int)len2;

                for(int j = 0; j <= n2; ++j) {
                    int index = (int)distance + j;
                    if (index < values.length) {
                        if (this.notFloat) {
                            values[index] = (float)ip.getInterpolatedPixel(rx, ry);
                        } else if (this.rgb) {
                            int rgbPixel = ((ColorProcessor)ip).getInterpolatedRGBPixel(rx, ry);
                            values[index] = Float.intBitsToFloat(rgbPixel & 16777215);
                        } else {
                            values[index] = (float)ip.getInterpolatedValue(rx, ry);
                        }
                    }

                    rx += xinc;
                    ry += yinc;
                }

                distance += len;
                leftOver = len2 - (double)n2;
            }
        }

        return values;
    }

    void doIrregularSetup(Roi roi) {
        this.n = ((PolygonRoi)roi).getNCoordinates();
        int[] ix = ((PolygonRoi)roi).getXCoordinates();
        int[] iy = ((PolygonRoi)roi).getYCoordinates();
        this.x = new double[this.n];
        this.y = new double[this.n];

        int i;
        for(i = 0; i < this.n; ++i) {
            this.x[i] = (double)ix[i];
            this.y[i] = (double)iy[i];
        }

        if (roi.getType() == 7) {
            for(i = 1; i < this.n - 1; ++i) {
                this.x[i] = (this.x[i - 1] + this.x[i] + this.x[i + 1]) / 3.0D + 0.5D;
                this.y[i] = (this.y[i - 1] + this.y[i] + this.y[i + 1]) / 3.0D + 0.5D;
            }
        }

        Rectangle r = roi.getBounds();
        this.xbase = r.x;
        this.ybase = r.y;
        this.length = 0.0D;
        this.segmentLengths = new double[this.n];
        this.dx = new double[this.n];
        this.dy = new double[this.n];

        for(int i_ = 0; i_ < this.n - 1; ++i_) {
            double xdelta = this.x[i_ + 1] - this.x[i_];
            double ydelta = this.y[i_ + 1] - this.y[i_];
            double segmentLength = Math.sqrt(xdelta * xdelta + ydelta * ydelta);
            this.length += segmentLength;
            this.segmentLengths[i_] = segmentLength;
            this.dx[i_] = xdelta;
            this.dy[i_] = ydelta;
        }

    }

    private float[] getLine(ImageProcessor ip, double x1, double y1, double x2, double y2, float[] data) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        int n = (int)Math.round(Math.sqrt(dx * dx + dy * dy));
        if (data == null) {
            data = new float[n];
        }

        double xinc = dx / (double)n;
        double yinc = dy / (double)n;
        double rx = x1;
        double ry = y1;

        for(int i = 0; i < n; ++i) {
            if (this.notFloat) {
                data[i] = (float)ip.getInterpolatedPixel(rx, ry);
            } else if (this.rgb) {
                int rgbPixel = ((ColorProcessor)ip).getInterpolatedRGBPixel(rx, ry);
                data[i] = Float.intBitsToFloat(rgbPixel & 16777215);
            } else {
                data[i] = (float)ip.getInterpolatedValue(rx, ry);
            }

            rx += xinc;
            ry += yinc;
        }

        return data;
    }

    private float[] getOrthoLine(ImageProcessor ip, int x1, int y1, int x2, int y2, float[] data) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int n = Math.max(Math.abs(dx), Math.abs(dy));
        if (data == null) {
            data = new float[n];
        }

        int xinc = dx / n;
        int yinc = dy / n;
        int rx = x1;
        int ry = y1;

        for(int i = 0; i < n; ++i) {
            if (this.notFloat) {
                data[i] = (float)ip.getPixel(rx, ry);
            } else if (this.rgb) {
                int rgbPixel = ((ColorProcessor)ip).getPixel(rx, ry);
                data[i] = Float.intBitsToFloat(rgbPixel & 16777215);
            } else {
                data[i] = ip.getPixelValue(rx, ry);
            }

            rx += xinc;
            ry += yinc;
        }

        return data;
    }

    void drawLine(double x1, double y1, double x2, double y2, ImagePlus imp) {
        ImageCanvas ic = imp.getCanvas();
        if (ic != null) {
            Graphics g = ic.getGraphics();
            g.setColor(new Color(1.0F, 1.0F, 0.0F, 0.4F));
            g.drawLine(ic.screenX((int)(x1 + 0.5D)), ic.screenY((int)(y1 + 0.5D)), ic.screenX((int)(x2 + 0.5D)), ic.screenY((int)(y2 + 0.5D)));
        }
    }

    public void textValueChanged(TextEvent e) {
        this.updateSize();
    }

    public void itemStateChanged(ItemEvent e) {
        if (IJ.isMacOSX()) {
            IJ.wait(100);
        }

        Checkbox cb = (Checkbox)this.checkboxes.elementAt(2);
        this.nointerpolate = cb.getState();
        this.updateSize();
    }

    void updateSize() {
        double outSpacing = Tools.parseDouble(((TextField)this.fields.elementAt(0)).getText(), 0.0D);
        int count = 0;
        boolean lineSelection = this.fields.size() == 2;
        if (lineSelection) {
            count = (int)Tools.parseDouble(((TextField)this.fields.elementAt(1)).getText(), 0.0D);
            if (count > 0) {
                this.makePolygon(count, outSpacing);
            }
        }

        String size = this.getSize(this.inputZSpacing, outSpacing, count);
        this.message.setText("Output Size: " + size);
    }

    String getSize(double inSpacing, double outSpacing, int count) {
        int size = this.getOutputStackSize(inSpacing, outSpacing, count);
        int mem = this.getAvailableMemory();
        String available = mem != -1 ? " (" + mem + "MB free)" : "";
        if (this.message != null) {
            this.message.setForeground(mem != -1 && size > mem ? Color.red : Color.black);
        }

        return size > 0 ? size + "MB" + available : "<1MB" + available;
    }

    void makePolygon(int count, double outSpacing) {
        int[] x = new int[4];
        int[] y = new int[4];
        Calibration cal = this.imp.getCalibration();
        double cx = cal.pixelWidth;
        double cy = cal.pixelHeight;
        x[0] = (int)this.gx1;
        y[0] = (int)this.gy1;
        x[1] = (int)this.gx2;
        y[1] = (int)this.gy2;
        double dx = this.gx2 - this.gx1;
        double dy = this.gy2 - this.gy1;
        double nrm = Math.sqrt(dx * dx + dy * dy) / outSpacing;
        double xInc = -(dy / (cx * nrm));
        double yInc = dx / (cy * nrm);
        x[2] = x[1] + (int)(xInc * (double)count);
        y[2] = y[1] + (int)(yInc * (double)count);
        x[3] = x[0] + (int)(xInc * (double)count);
        y[3] = y[0] + (int)(yInc * (double)count);
        this.imp.setRoi(new PolygonRoi(x, y, 4, 3));
    }

    int getOutputStackSize(double inSpacing, double outSpacing, int count) {
        Roi roi = this.imp.getRoi();
        int width = this.imp.getWidth();
        int height = this.imp.getHeight();
        if (roi != null) {
            Rectangle r = roi.getBounds();
            width = r.width;
            width = r.height;
        }

        int type = roi != null ? roi.getType() : 0;
        int stackSize = this.imp.getStackSize();
        double size = 0.0D;
        if (type == 0) {
            size = (double)(width * height * stackSize);
            if (outSpacing > 0.0D && !this.nointerpolate) {
                size *= inSpacing / outSpacing;
            }
        } else {
            size = this.gLength * (double)count * (double)stackSize;
        }

        int bits = this.imp.getBitDepth();
        switch(bits) {
            case 16:
                size *= 2.0D;
                break;
            case 24:
            case 32:
                size *= 4.0D;
        }

        return (int)Math.round(size / 1048576.0D);
    }

    int getAvailableMemory() {
        long max = IJ.maxMemory();
        if (max == 0L) {
            return -1;
        } else {
            long inUse = IJ.currentMemory();
            long available = max - inUse;
            return (int)((available + 524288L) / 1048576L);
        }
    }

    static {
        startAtS = starts[0];
        sliceCountS = 1;
    }
}
