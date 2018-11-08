package com.ahqlab.hodooopencv.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.domain.HodooWrapping;
import com.ahqlab.hodooopencv.presenter.interfaces.HodooCameraPresenter;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class HodooCameraPresenterImpl implements HodooCameraPresenter.Precenter {
    private final String TAG = HodooCameraPresenterImpl.class.getSimpleName();
    private HodooCameraPresenter.VIew mView;
    public HodooCameraPresenterImpl (HodooCameraPresenter.VIew view) {
        mView = view;
        mView.setPresenter(this);
    }

    @Override
    public void wrappingProcess(HodooWrapping wrapping) {

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Mat inputMat = Imgcodecs.imread(wrapping.getFileName(), Imgcodecs.CV_LOAD_IMAGE_COLOR), grayMat, tempMat, contourMat, inputProcMat, outputProcMat, mTransMat, resultMat;
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_BGR2RGBA);

        tempMat = contourMat = resultMat = new Mat();
        grayMat = inputMat.clone();
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(11, 11), 2);
        Imgproc.Canny(grayMat, tempMat, 80, 120);
        Imgproc.dilate(tempMat, contourMat, new Mat(), new Point(-1, -1), 1);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(tempMat, contours, contourMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for ( int i = 0; i < contours.size(); i++ ) {
            MatOfPoint cnt = contours.get(i);
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
            Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true); //다각형 검출

            Rect rect = Imgproc.boundingRect(cnt);
            if (rect.width > 300) { // 일정 면적일 경우 실행
//                Imgproc.drawContours(mImage, contours, -1, new Scalar(255, 255, 255), Core.FILLED);
//                mRgba = mImgInput.clone();

                if ( approxCurve.total() == 4 ) {
                    Point point1 = approxCurve.toArray()[0]; //오른쪽위
                    Point point2 = approxCurve.toArray()[1]; //왼쪽위
                    Point point3 = approxCurve.toArray()[2]; //왼쪽아래
                    Point point4 = approxCurve.toArray()[3]; //오른쪽아래


                    inputProcMat = new Mat(4, 1, CvType.CV_32FC2);
                    outputProcMat = new Mat(4, 1, CvType.CV_32FC2);

                    //왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래
                    inputProcMat.put(0, 0, point2.x, point2.y, point1.x, point1.y, point4.x, point4.y, point3.x, point3.y);
                    outputProcMat.put(0, 0, 0, 0, inputMat.cols() - 1, 0, inputMat.cols() - 1, inputMat.rows() - 1, 0, inputMat.rows() - 1);

                    int originWidth = inputMat.width();
                    double x2 = originWidth - point2.x - (point2.x + point1.x);

                    Size point = new Size(inputMat.width() - point2.x + point1.x, inputMat.height() - point3.y + point4.y);

                    mTransMat = Imgproc.getPerspectiveTransform(inputProcMat, outputProcMat);
                    Imgproc.warpPerspective(inputMat, resultMat, mTransMat, inputMat.size());
//                    Rect box = Imgproc.boundingRect();
                    if ( resultMat.width() > resultMat.height() ) {
                        Bitmap bitmap = Bitmap.createBitmap( resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888 );
                        Utils.matToBitmap(resultMat, bitmap);
                        mView.setWrappingImg(bitmap);
                    }

                }
            }
        }
    }

    @Override
    public void saveWrappingImg(Context context, Bitmap bitmap) {
        String root = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + context.getString(R.string.app_name);
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "test.jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            mView.saveImgResult(true, file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            mView.saveImgResult(false, "");
        }
    }
}
