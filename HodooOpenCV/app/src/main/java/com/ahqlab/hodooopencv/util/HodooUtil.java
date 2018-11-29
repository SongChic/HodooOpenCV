package com.ahqlab.hodooopencv.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;

public class HodooUtil {
    private static final String TAG = HodooUtil.class.getSimpleName();
    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
    public static Bitmap createShadowBitmap(Bitmap originalBitmap) {
        Canvas c = new Canvas(originalBitmap);
        Paint mShadow = new Paint();
// radius=10, y-offset=2, color=black
        mShadow.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
// in onDraw(Canvas)
        c.drawBitmap(originalBitmap, 0.0f, 0.0f, mShadow);
        return originalBitmap;
    }
    public static int compareFeature(String filename1) {
        String target = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "target.jpg";

        int retVal = 0;
        long startTime = System.currentTimeMillis();

        // Load images to compare
        Mat img1 = Imgcodecs.imread(filename1, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Mat img2 = Imgcodecs.imread(target, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        // Declare key point of images
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        // Definition of ORB key point detector and descriptor extractors
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        // Detect key points
        detector.detect(img1, keypoints1);
        detector.detect(img2, keypoints2);

        // Extract descriptors
        extractor.compute(img1, keypoints1, descriptors1);
        extractor.compute(img2, keypoints2, descriptors2);

        // Definition of descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // Match points of two images
        MatOfDMatch matches = new MatOfDMatch();
//  System.out.println("Type of Image1= " + descriptors1.type() + ", Type of Image2= " + descriptors2.type());
//  System.out.println("Cols of Image1= " + descriptors1.cols() + ", Cols of Image2= " + descriptors2.cols());

        // Avoid to assertion failed
        // Assertion failed (type == src2.type() && src1.cols == src2.cols && (type == CV_32F || type == CV_8U)
        if (descriptors2.cols() == descriptors1.cols()) {
            matcher.match(descriptors1, descriptors2 ,matches);

            // Check matches of key points
            DMatch[] match = matches.toArray();
            double max_dist = 0; double min_dist = 100;

            for (int i = 0; i < descriptors1.rows(); i++) {
                double dist = match[i].distance;
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }
            if ( DEBUG ) Log.e(TAG, String.format("max_dist : %f, min_dist : %f", max_dist, min_dist));

            // Extract good images (distances are under 10)
            for (int i = 0; i < descriptors1.rows(); i++) {
//                Imgproc.circle(drawMat, descriptors1.);
                if ( DEBUG ) Log.e(TAG, String.format("match[i].distance : %f", match[i].distance));
                if (match[i].distance <= 20) {
                    retVal++;
                }
            }
            if ( DEBUG ) Log.e(TAG, String.format("retVal : %d", retVal));
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("estimatedTime=" + estimatedTime + "ms");

        return retVal;
    }

}
