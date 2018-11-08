package com.ahqlab.hodooopencv.activity;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.adapter.ColorListAdapter;
import com.ahqlab.hodooopencv.base.BaseActivity;
import com.ahqlab.hodooopencv.databinding.ActivityAnalsisBinding;
import com.ahqlab.hodooopencv.domain.HodooFindColor;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

public class AnalysisActivity extends BaseActivity<AnalysisActivity> {
    private ActivityAnalsisBinding binding;
    private List<HodooFindColor> colors;
    private Mat inputImg;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_analsis);
        String path = getIntent().getStringExtra("path");
        if ( path == null || path.equals("")) {
            path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ) + File.separator + getString( R.string.app_name ) + File.separator + "test.jpg";
        }
        Mat grayMat, resultMat, tempMat, rgbColor, hsvColor, contourMat, cannyMat, hovMat, nlabels, stats, invBinarized, labels, centroids;
        inputImg = Imgcodecs.imread(path);
        Imgproc.cvtColor(inputImg, inputImg, Imgproc.COLOR_BGR2RGBA);
        colors = new ArrayList<>();

        setBitmap(inputImg);

        OpenCVAsync async = new OpenCVAsync();
        async.execute(inputImg);

        if ( DEBUG ) return;
        MatOfPoint2f approxCurve = new MatOfPoint2f();

//        hsvColor = contourMat = tempMat = resultMat = inputImg.clone();

        resultMat = hovMat = new Mat();


        grayMat = inputImg.clone();
        Imgproc.cvtColor(grayMat, grayMat, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(11, 11), 2);
        Imgproc.Canny(grayMat, resultMat, 80, 0);

        Imgproc.dilate(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(4, 4)), new Point(-1, -1), 3);
        Imgproc.erode(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(3, 3)), new Point(-1, -1), 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(resultMat, contours, hovMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if ( DEBUG ) Log.e(TAG, String.format("contours size : %d", contours.size()));

        for ( int i = 0; i < contours.size(); i++ ) {
            MatOfPoint cnt = contours.get(i);
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true); //다각형 검출


//            Imgproc.drawContours(inputImg, contours, i, new Scalar(0, 0, 0), 10);
            Rect rect = Imgproc.boundingRect(cnt);
            if ( DEBUG ) Log.e(TAG, String.format("rect width : %d", rect.width));
            if ( (rect.width > 300 && rect.width < 400) && approxCurve.total() >= 4 ) {

//                for ( int a = 0; a < inputImg.cols(); a++ ) {
//                    for ( int b = 0; b < inputImg.rows(); b++ ) {
//                        Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
//                        mSepiaKernel.put(a, b, 1);
//                    }
//                }

                int x = (int)(approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2;
                int y = (int)(approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2;
                double R=0,G=0,B=0;
                double[] color = inputImg.get(y, x);

                Log.e(TAG, "----------------------------------------------------");
                R = color[0];
                G= color[1];
                B = color[2];
                HodooFindColor findColor = HodooFindColor.builder().red((int) R).green((int) G).blue((int) B).index(i).build();
                colors.add(findColor);
                Log.e(TAG, String.format("index : %d, red : %f, green : %f, blue : %f", i, R, G, B));
                Log.e(TAG, "----------------------------------------------------");

//                double[] color = inputImg.get( (int) (approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2, (int) (approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2 );
                Point point = new Point(x, y);
                Imgproc.circle(inputImg, point, 50, new Scalar(0, 0, 255), 50);
                Imgproc.putText(inputImg, String.valueOf( i ), point, Core.FONT_HERSHEY_SIMPLEX, 5, new Scalar(255, 0, 0), 10);
//                double[] color = inputImg.get( (int)point.x, (int)point.y);
////                Point point = new Point((int) (approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2,(int) (approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2 );
//
//                int rows = inputImg.rows();
//                int cols = inputImg.cols();
//                // int ch = mRgba.channels();
//                double R=0,G=0,B=0;
//
//
//                for (int a=0; a<rows; a++)
//                {
//                    for (int j=0; j<cols; j++)
//                    {
//                        Log.e(TAG, "----------------------------------------------------");
//                        double[] data = inputImg.get(a, j); //Stores element in an array
//                        R = data[0];
//                        G= data[1];
//                        B = data[2];
//                        Log.e(TAG, String.format("red : %f, green : %f, blue : %f", R, G, B));
//                        Log.e(TAG, "----------------------------------------------------");
//                    }
//                }

//                for (int j = 0; j < approxCurve.total(); j++) {
//                    Point circlePoint = approxCurve.toArray()[j];
//                    Imgproc.circle(inputImg, circlePoint, 50, new Scalar(0, 0, 255), 50);
////                Imgproc.putText(inputImg, String.valueOf( j + 1 ), point, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
//                }
            }
        }

        setBitmap(inputImg);



//        ColorBlobDetector detector = new ColorBlobDetector();
//        Scalar rega = new Scalar(255), CONTOUR_COLOR = new Scalar(255,0,0,255), mBlobColorHsv = new Scalar(235, 75.2, 45.9, 0.0);
//        detector.setHsvColor(mBlobColorHsv);
//
//        MatOfPoint2f approxCurve = new MatOfPoint2f();
//
//        hovMat = stats = invBinarized = labels = centroids = nlabels = new Mat();
//        cannyMat = new Mat(inputImg.width(), inputImg.height(), CvType.CV_8UC4);
//
////        Imgproc.connectedComponentsWithStats(invBinarized, labels, stats, centroids, CvType.CV_32S, Imgproc.CCL_WU);
//
//
//        Imgproc.cvtColor(inputImg, inputImg, Imgproc.COLOR_BGR2RGBA);
////        Imgproc.cvtColor(inputImg, hsvColor, Imgproc.COLOR_RGB2HSV);
//        Imgproc.cvtColor(inputImg, grayMat, Imgproc.COLOR_BGRA2GRAY);
//        Imgproc.GaussianBlur(grayMat, grayMat, new Size(11, 11), 0);
//
////        Core.inRange(hsvColor, new Scalar(50, 50, 50), new Scalar(255, 255, 255), resultMat);
//
//        Imgproc.adaptiveThreshold(grayMat, resultMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 1);
////        Core.inRange(resultMat, new Scalar(50, 50, 50), new Scalar(255, 255, 255), resultMat);
//////        Imgproc.Canny(grayMat, resultMat, 90, 120);
//
//        Imgproc.Canny(resultMat, cannyMat, 20, 90);
////        Imgproc.dilate(cannyMat, cannyMat, new Mat(), new Point(-1, -1), 1); //노이즈 제거
//
//        Imgproc.dilate(cannyMat, cannyMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(4, 4)), new Point(-1, -1), 3);
//        Imgproc.erode(cannyMat, cannyMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(3, 3)), new Point(-1, -1), 3);
//
//        List<MatOfPoint> contours = new ArrayList<>();
////        Imgproc.findContours(cannyMat, contours, hovMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        Imgproc.findContours(cannyMat, contours, hovMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        for ( int i = 0; i < contours.size(); i++ ) {
//            MatOfPoint cnt = contours.get(i);
//            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
//            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true); //다각형 검출
//
//            Imgproc.drawContours(inputImg, contours, -1, new Scalar(255, 0, 0), 5);
//        }
//
//        if ( DEBUG ) Log.e(TAG, String.format("contours size : %d", contours.size()));




        /* 모폴로지 연산 */
//        int morph_size = 2;
//        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 2*morph_size + 1, 2*morph_size+1 ), new Point( morph_size, morph_size ) );
//        Imgproc.morphologyEx(resultMat, resultMat, MORPH_TOPHAT, element, new Point(-1, -1));
//
//        Imgproc.dilate(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(5, 5)));
//        Imgproc.erode(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(5, 5)));
//        Core.bitwise_not(resultMat, resultMat);

//
////        Imgproc.dilate(resultMat, resultMat, new Mat(), new Point(-1, -1), 1);
//

//

//
//
//
//        if ( DEBUG ) Log.e(TAG, String.format("contours : %d", contours.size()));
//        Imgproc.drawContours(resultMat, contours, -1, new Scalar(255, 255, 255), Core.FILLED);

//        Photo.fastNlMeansDenoising(resultMat, resultMat, 11, 31, 9);


//        Imgproc.threshold(grayMat, resultMat, 100, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
//        Core.inRange(hsvColor, new Scalar(0, 0, 0), new Scalar(255,255,80), resultMat);

//        Imgproc.GaussianBlur(mGrayMat, mGrayMat, new Size(11, 11), 2);
//        Imgproc.Canny(mGrayMat, mImgResult, 90, 120);
//        Imgproc.dilate(mImgResult, mImgResult, new Mat(), new Point(-1, -1), 1);



    }
    public void setBitmap( Mat result ) {
        Bitmap bitmap = Bitmap.createBitmap( result.cols(), result.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(result, bitmap);
        binding.resultImg.setImageBitmap(bitmap);
        if ( colors.size() > 0 ) {
            ColorListAdapter adapter = new ColorListAdapter(this, colors);
            binding.colorList.setAdapter(adapter);
        }
    }

    private class OpenCVAsync extends AsyncTask<Mat, Void, Mat> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            binding.progressWrap.setVisibility(View.VISIBLE);
        }

        @Override
        protected Mat doInBackground(Mat... mat) {
            Mat inputMat = mat[0];
            Mat grayMat, resultMat, tempMat, rgbColor, hsvColor, contourMat, cannyMat, hovMat, nlabels, stats, invBinarized, labels, centroids;
            grayMat = resultMat = hovMat = new Mat();
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGRA2GRAY);
            Imgproc.GaussianBlur(grayMat, grayMat, new Size(11, 11), 2);
//            Imgproc.adaptiveThreshold(grayMat, resultMat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY,15, 40);
            Core.inRange(grayMat, new Scalar(0, 200 ,0), new Scalar(220, 220, 220), resultMat);
            Imgproc.Canny(grayMat, resultMat, 20, 0);





            Imgproc.dilate(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(4, 4)), new Point(-1, -1), 3);
            Imgproc.erode(resultMat, resultMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(3, 3)), new Point(-1, -1), 3);
            if ( DEBUG ) return resultMat;

            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(resultMat, contours, hovMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            if ( DEBUG ) Log.e(TAG, String.format("contours size : %d", contours.size()));

            for ( int i = 0; i < contours.size(); i++ ) {
                MatOfPoint cnt = contours.get(i);
                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

                if ( Imgproc.isContourConvex(cnt) ) {
                    Log.e(TAG, "isContourConvex");
                }

                Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true); //다각형 검출


            Imgproc.drawContours(inputImg, contours, -1, new Scalar(0, 0, 0), -1);
                Rect rect = Imgproc.boundingRect(cnt);
                if ( DEBUG ) Log.e(TAG, String.format("rect width : %d", rect.width));
                if ( (rect.width > 15 && rect.width < 50) && approxCurve.total() >= 4 ) {

//                for ( int a = 0; a < inputImg.cols(); a++ ) {
//                    for ( int b = 0; b < inputImg.rows(); b++ ) {
//                        Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
//                        mSepiaKernel.put(a, b, 1);
//                    }
//                }

                    int x = (int)(approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2;
                    int y = (int)(approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2;
                    double R=0,G=0,B=0;
                    double[] color = inputImg.get(y, x);

                    Log.e(TAG, "----------------------------------------------------");
                    R = color[0];
                    G= color[1];
                    B = color[2];
                    HodooFindColor findColor = HodooFindColor.builder().red((int) R).green((int) G).blue((int) B).index(i).build();
                    colors.add(findColor);
                    Log.e(TAG, String.format("index : %d, red : %f, green : %f, blue : %f", i, R, G, B));
                    Log.e(TAG, "----------------------------------------------------");

//                double[] color = inputImg.get( (int) (approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2, (int) (approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2 );
                    Point point = new Point(x, y);
                    Imgproc.circle(inputImg, point, 50, new Scalar(0, 0, 255), 50);
                    Imgproc.putText(inputImg, String.valueOf( i ), point, Core.FONT_HERSHEY_SIMPLEX, 5, new Scalar(255, 0, 0), 10);
//                double[] color = inputImg.get( (int)point.x, (int)point.y);
////                Point point = new Point((int) (approxCurve.toArray()[1].x + approxCurve.toArray()[0].x) / 2,(int) (approxCurve.toArray()[1].y + approxCurve.toArray()[3].y) / 2 );
//
//                int rows = inputImg.rows();
//                int cols = inputImg.cols();
//                // int ch = mRgba.channels();
//                double R=0,G=0,B=0;
//
//
//                for (int a=0; a<rows; a++)
//                {
//                    for (int j=0; j<cols; j++)
//                    {
//                        Log.e(TAG, "----------------------------------------------------");
//                        double[] data = inputImg.get(a, j); //Stores element in an array
//                        R = data[0];
//                        G= data[1];
//                        B = data[2];
//                        Log.e(TAG, String.format("red : %f, green : %f, blue : %f", R, G, B));
//                        Log.e(TAG, "----------------------------------------------------");
//                    }
//                }

//                for (int j = 0; j < approxCurve.total(); j++) {
//                    Point circlePoint = approxCurve.toArray()[j];
//                    Imgproc.circle(inputImg, circlePoint, 50, new Scalar(0, 0, 255), 50);
////                Imgproc.putText(inputImg, String.valueOf( j + 1 ), point, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
//                }
                }
            }
//            if ( DEBUG ) return resultMat;
            return inputImg;
        }

        @Override
        protected void onPostExecute(Mat result) {
            super.onPostExecute(result);
            setBitmap(result);
            binding.progressWrap.setVisibility(View.GONE);
        }
    }

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
}
