package com.ahqlab.hodooopencv.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.FlannBasedMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;
import static org.opencv.core.Core.NORM_HAMMING;
import static org.opencv.core.Core.NORM_L2;

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
        Mat img1 = Imgcodecs.imread(target, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Mat img2 = Imgcodecs.imread(filename1, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        Mat gray1 = new Mat();
        Mat gray2 = new Mat();
        Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_RGB2GRAY);



        // Declare key point of images
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        // Definition of ORB key point detector and descriptor extractors
        FastFeatureDetector featureDetector = FastFeatureDetector.create();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        // Detect key points
        detector.detect(gray1, keypoints1);
        detector.detect(gray2, keypoints2);

        // Extract descriptors
        extractor.compute(gray1, keypoints1, descriptors1);
        extractor.compute(gray2, keypoints2, descriptors2);

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
//                if ( DEBUG ) Log.e(TAG, String.format("match[i].distance : %f", match[i].distance));
                if (match[i].distance <= 20) {
                    retVal++;
                }
            }
            if ( DEBUG ) Log.e(TAG, String.format("retVal : %d", retVal));
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        MatOfByte drawnMatches = new MatOfByte();

        Mat imgMatches = new Mat();
        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches, imgMatches, new Scalar(0, 255, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "result.jpg", imgMatches);


        return retVal;
    }
    public static int compareFeature2 ( Context context, String fileName ) {

        String target = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "target.jpg";

        Mat img1 = Imgcodecs.imread(target, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Mat img2 = Imgcodecs.imread(fileName, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Mat imgMatches = new Mat();

//        ORB orb = ORB.create();
//
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint(), keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
//
//        orb.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
//        orb.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);
//
//        BFMatcher matcher = new BFMatcher(NORM_L2, true);
        MatOfDMatch matches = new MatOfDMatch();
//        matcher.match(descriptors1, descriptors2, matches, new Mat());
//
//        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches, imgMatches, new Scalar(0, 255, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);


//        int num = 500;
//        MatOfPoint corners = new MatOfPoint();
//        Imgproc.goodFeaturesToTrack(img1, corners, num, 0.1, 5);
//        for (int i = 0; i < corners.total(); i++) {
//            Imgproc.circle(img1, corners.toArray()[i], 5, new Scalar(255, 0, 0), 3);
//        }
//        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "result.jpg", imgMatches);
//
//        if ( DEBUG ) return;

        FeatureDetector surf = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        surf.detect(img1, keypoints1);
        surf.detect(img2, keypoints2);

        extractor.compute(img1, keypoints1, descriptors1);
        extractor.compute(img2, keypoints1, descriptors2);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);


        matcher.match(descriptors1, descriptors2, matches);

        DMatch[] match = matches.toArray();

        double max_dist = 0; double min_dist = 100;
        for (int i = 0; i < descriptors1.rows(); i++) {
            double dist = matches.toArray()[i].distance;
            if( dist < min_dist ) min_dist = dist;
            if( dist > max_dist ) max_dist = dist;
        }
        List<DMatch> goodMatches = new ArrayList<>();
        for (int i = 0; i < descriptors1.rows(); i++) {
            if ( matches.toArray()[i].distance <= Math.max(2*min_dist, 0.02) )
                goodMatches.add(matches.toArray()[i]);
        }

        int retVal = 0;
        if ( DEBUG ) Log.e(TAG, String.format("max_dist : %f, min_dist : %f", max_dist, min_dist));
        for (int i = 0; i < descriptors1.rows(); i++) {
            if (match[i].distance <= 30) {
                retVal++;
            }
        }
        if ( DEBUG ) Log.e(TAG, String.format("two retVal : %d", retVal));
        MatOfByte drawnMatches = new MatOfByte();

        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches, imgMatches, new Scalar(0, 255, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "result.jpg", imgMatches);
        return retVal;
    }

}
