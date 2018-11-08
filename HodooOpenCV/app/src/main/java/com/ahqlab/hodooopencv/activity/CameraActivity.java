package com.ahqlab.hodooopencv.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.base.BaseActivity;
import com.ahqlab.hodooopencv.databinding.ActivityCameraBinding;
import com.ahqlab.hodooopencv.databinding.ActivityMainBinding;
import com.ahqlab.hodooopencv.domain.HodooRect;
import com.ahqlab.hodooopencv.domain.HodooWrapping;
import com.ahqlab.hodooopencv.presenter.HodooCameraPresenterImpl;
import com.ahqlab.hodooopencv.presenter.interfaces.HodooCameraPresenter;
import com.ahqlab.hodooopencv.view.HodooJavaCamera;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

public class CameraActivity extends BaseActivity<CameraActivity> implements CameraBridgeViewBase.CvCameraViewListener2, HodooCameraPresenter.VIew {
    private ActivityCameraBinding binding;
    private Mat mImgInput, mImgResult, mImgGray, mRgba, hovIMG, dsIMG, usIMG, cIMG, croppedMat;
    private MatOfPoint2f approxCurve;
    private HodooWrapping mWrapping;
    private HodooCameraPresenter.Precenter mPrecenter;
    private Bitmap warppingResult;
    String[] name = {
            "none",
            "none",
            "none",
            "triangle",
            "quadrangle",
            "pentagon",
            "hexagon",
            "heptagon",
            "nonagon",
            "decagon"
    };


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback( this ) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch ( status ) {
                case LoaderCallbackInterface.SUCCESS :
                    mImgResult = mImgGray = mRgba = dsIMG = usIMG = cIMG = hovIMG = croppedMat = new Mat();
                    approxCurve = new MatOfPoint2f();
                    binding.hodooCameraView.enableView();
                    break;
                    default:
                        super.onManagerConnected(status);
                        break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        binding.setActivity(this);
        mPrecenter = new HodooCameraPresenterImpl(this);
//        binding.hodooCameraView.start();
        /* permission check (s) */
        /* permission check (e) */

        binding.hodooCameraView.setCvCameraViewListener(this);
        binding.hodooCameraView.setCameraIndex(0);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e(TAG, String.format("width : %d, height : %d", width, height));
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


        mImgInput = inputFrame.rgba();
//        mImgInput.convertTo(mImgInput, CvType.CV_8UC3);
//        Imgproc.cvtColor(mImgInput, mImgInput, Imgproc.COLOR_BGRA2GRAY);
        mImgGray = inputFrame.gray();
//        if ( DEBUG )
//            return mImgInput;
//        convertRGBtoGray(mImgInput.getNativeObjAddr(), mImgGray.getNativeObjAddr());

        Imgproc.pyrDown(mImgGray, dsIMG, new Size(mImgGray.cols() / 2, mImgGray.rows() / 2));
        Imgproc.pyrUp(dsIMG, usIMG, mImgGray.size());

        mRgba = mImgGray.clone();
        Imgproc.GaussianBlur(mImgGray, mRgba, new Size(11, 11), 2);
        Imgproc.Canny(mRgba, mImgResult, 200, 300); //윤곽선만 가져오기
//        Imgproc.threshold(mRgba, mImgResult, 90, 255, THRESH_BINARY_INV | THRESH_OTSU);
//        if ( DEBUG ) {
//            return mImgResult;
//        }
//        Imgproc.erode(mImgResult, mImgResult, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//        Imgproc.erode(mImgResult, mImgResult, new Mat(), new Point(3, 3), 1); //노이즈 제거
        Imgproc.dilate(mImgResult, mImgResult, new Mat(), new Point(-1, -1), 1); //노이즈 제거

//        if( DEBUG ) return mImgResult;

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        cIMG = mImgResult.clone();

//        if ( DEBUG ) return mImgResult;

        Imgproc.findContours(cIMG, contours, hovIMG, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); //윤곽선 검출
        for ( int i = 0; i < contours.size(); i++ ) {

//            if ( DEBUG ) Log.e(TAG, String.format("contours.size() : %d", contours.size()));
            MatOfPoint cnt = contours.get(i);
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true); //다각형 검출
//            double peri = Imgproc.arcLength(curve, true);
            if ( DEBUG ) Log.e(TAG, String.format("approxCurve total : %d", approxCurve.total()));


//            Log.e(TAG, String.format("approxCurve : %d", approxCurve.total()));
            Rect rect = Imgproc.boundingRect(cnt);
            Log.e(TAG, String.format("size : %d", rect.width));
            if ( rect.width > 300 && rect.width != 1920 /* 디바이스 넓이로 대체 */ ) { // 일정 면적일 경우 실행
                int size = (int) approxCurve.total();
                if ( size >= 4 ) {

                        List<Point> points = new ArrayList<>();

                        mRgba = mImgInput.clone();
                        for ( int j = 0; j < approxCurve.total(); j++ ) {
                            Point point = approxCurve.toArray()[j];
                            points.add(point);
                            Imgproc.circle(mImgInput, point, 20, new Scalar(255, 0, 0), 10, Core.FILLED);
                            Imgproc.putText(mImgInput, String.valueOf( j + 1 ), point, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
                        }



                        Point point1 = approxCurve.toArray()[0];
                        Point point2 = approxCurve.toArray()[1];
                        Point point3 = approxCurve.toArray()[2];
                        Point point4 = approxCurve.toArray()[3];


                        points.add(point1);
                        points.add(point2);
                        points.add(point3);
                        points.add(point4);
                    mWrapping = HodooWrapping.builder().points(points).build();

//                    croppedMat = mImgResult.submat(0, 10, 0, 10 );
//                        Imgproc.putText(mImgInput, "1", point1, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
//                        Imgproc.putText(mImgInput, "2", point2, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
//                        Imgproc.putText(mImgInput, "3", point3, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);
//                        Imgproc.putText(mImgInput, "4", point4, Core.FONT_HERSHEY_SIMPLEX, 3, new Scalar(255, 0, 0), 3);

//                        Imgproc.line(mImgInput, point1, point4, new Scalar(0, 0, 255), 10);
//                        Imgproc.line(mImgInput, point2, point3, new Scalar(0, 0, 255), 10);

//                        Imgproc.rectangle(mImgInput, new Point(point1.x, point1.x + rect.width), new Point(point1.y, point1.y + rect.y), new Scalar(0, 0, 255), 10);

//                        Imgproc.rectangle(mImgInput, point1, point2, new Scalar(255, 0, 0), 0, Core.FILLED);


                        Imgproc.drawContours(mImgInput, contours, -1, new Scalar(255, 255, 255), Core.FILLED);

//                        Imgproc.warpPerspective(mImgInput, croppedMat, new Mat(), new Size(512,512));

//                        Imgproc.accumulateWeighted(mImgInput, mImgInput , 0.3);


//                        Imgproc.circle(mImgInput, poin1, 50, new Scalar(255, 0, 0), 10);
//                        Imgproc.circle(mImgInput, point2, 50, new Scalar(255, 0, 0), 10);
//                        Imgproc.circle(mImgInput, point3, 50, new Scalar(255, 0, 0), 10);
//                        Imgproc.circle(mImgInput, point4, 50, new Scalar(255, 0, 0), 10);

//                        List<HodooRect> rects = new ArrayList<>();
//                        for ( int j = 0; j < points.size(); j++ ) {
//
//                            HodooRect rect = HodooRect.builder().x((float) points.get(j).y).y((float) points.get(j).x).build();
//                            rects.add(rect);
//                        }
//                        binding.maskView.setRect(rects);
//                    }


//                    Imgproc.line(mImgInput, approxCurve.toArray()[0], approxCurve.toArray()[(int) (approxCurve.total() - 1)], new Scalar(255, 0, 0), 10);
                    for (int k = 0; k < size - 1; k++) {
//                        Point point3 = approxCurve.toArray()[k];
//                        Point point4 = approxCurve.toArray()[k + 1];



//                        Imgproc.line(mImgInput, point1, point3, new Scalar(255, 0, 0), 10);
//                        binding.maskView.setPoint((float) approxCurve.toArray()[k].x, (float) approxCurve.toArray()[k].y);
//                        Imgproc.circle(mImgInput, approxCurve.toArray()[k], 50, new Scalar(255, 0, 0), 10);
//                        Imgproc.circle(mImgInput, approxCurve.toArray()[k + 1], 50, new Scalar(255, 0, 0), 10);
                    }

                }
//                if ( Imgproc.isContourConvex(cnt) ) {
//
//                }
            }
        }


//        for (MatOfPoint cnt : contours) {
//
//
//            double contourArea = Imgproc.contourArea(cnt);
//            if (Math.abs(contourArea) < 200) { //일정 면적 계산
//                continue;
//            }
//            int numberVertices = (int) approxCurve.total(); //검출 결과
//            //Rectangle detected
//            if (numberVertices >= 3 && numberVertices <= 6) {
//
//
//                List<Double> cos = new ArrayList<>();
//                for (int j = 2; j < numberVertices + 1; j++) {
//
//                    Point pointX = approxCurve.toArray()[j % numberVertices];
//                    Point pointY = approxCurve.toArray()[j - 2];
//                    Rect r = Imgproc.boundingRect(cnt);
//
//                    Imgproc.rectangle(mImgInput, pointX, pointY, new Scalar(255, 0, 0, 255), 5);
////                    Imgproc.circle(mImgInput, pointX, 10, new Scalar(255, 0, 0, 255), 3); //선그리기
////                    cos.add(angle(approxCurve.toArray()[j % numberVertices], approxCurve.toArray()[j - 2], approxCurve.toArray()[j - 1]));
//                }
//
//            }
//        }

        return mImgInput;
    }
    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public void onClick ( View v ) {
        switch ( v.getId() ) {
            case R.id.take_picture :
                /*  */
                    binding.hodooCameraView.takePicture(getString(R.string.app_name), "/test.jpg", new HodooJavaCamera.CameraCallback() {
                        @Override
                        public void onResult(final String fileName) {
                            if ( DEBUG ) Log.e(TAG, "fileName : " + fileName);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mWrapping.setFileName(fileName);
                                    mPrecenter.wrappingProcess(mWrapping);
                                }
                            });
                        }
                    });
                break;
            case R.id.use_picture :
                if ( warppingResult != null )
                    mPrecenter.saveWrappingImg(this, warppingResult);
                break;
        }
    }

    public void transfrom ( List<Point> points ) {
        double w1 = Math.sqrt( Math.pow( points.get(2).x - points.get(3).x, 2 ) ) + Math.sqrt( Math.pow( points.get(2).x - points.get(3).x, 2 ) );


//        Imgproc.getPerspectiveTransform()
    }

    public native void convertRGBtoGray ( long matAddrInput, long matAddrResult );
    public native void convertBinary ( long matGray );

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    @Override
    public void setWrappingImg(Bitmap resultMat) {
        final ImageView pictureView = new ImageView(CameraActivity.this);
//                                File file = new File(fileName);
        pictureView.setImageBitmap(resultMat);
        binding.maskView.addView(pictureView);
        binding.maskView.setVisibility(View.VISIBLE);
        binding.maskView.invalidate();
        warppingResult = resultMat;

//        new Handler().postDelayed(new Runnable() {
//            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//            @Override
//            public void run() {
//                pictureView.animate().scaleX(.1f).scaleY(.1f).translationX(.1f).translationY(.1f).withLayer().withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        binding.maskView.removeView(pictureView);
//                    }
//                });
//            }
//        }, 2000);
    }

    @Override
    public void saveImgResult(boolean state, String path) {
        if ( state ) {
            Intent intent = new Intent(CameraActivity.this, AnalysisActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        } else {
            Toast.makeText(this, "사진 저장에 실패했습니다.\n잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void setPresenter(HodooCameraPresenter.Precenter presenter) {

    }
}
