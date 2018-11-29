package com.ahqlab.hodooopencv.presenter;

/*
 * 2018.11.15 AHQLab
 * 제작 : 송석우
 *
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.domain.ComburResult;
import com.ahqlab.hodooopencv.domain.HodooFindColor;
import com.ahqlab.hodooopencv.domain.HsvValue;
import com.ahqlab.hodooopencv.http.RetrofitService;
import com.ahqlab.hodooopencv.presenter.interfaces.AnalysisPresenter;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;

public class AnalysisPresenterImpl implements AnalysisPresenter.Precenter {
    private final static String TAG = AnalysisPresenterImpl.class.getSimpleName();
    private AnalysisPresenter.VIew mView;
    private Mat originalMat;
    public AnalysisPresenterImpl ( AnalysisPresenter.VIew view ) {
        mView = view;
        mView.setPresenter(this);
    }
    @Override
    public void imageProcessing(String path) {
        Mat readMat = readMatImg(path);

//        if ( DEBUG ) Log.e(TAG, String.format("feature : %d", feature));



        Imgproc.cvtColor(readMat, readMat, Imgproc.COLOR_BGR2RGB);
        mView.setImage(convertMatToBitmap(readMat));
        OpenCVAsync async = new OpenCVAsync();
        async.execute(readMat);
    }

    private class OpenCVAsync extends AsyncTask<Mat, Void, Mat> {
        private List<HodooFindColor> colors;
        private int litmusBoxNum = 11;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mView.setProgressLayout(View.VISIBLE);
            colors = new ArrayList<>();
        }

        @Override
        protected Mat doInBackground(Mat... mats) {

            Mat inputMat = mats[0];

            originalMat = new Mat();
            inputMat.copyTo(originalMat);
//
//            int feature = compareFeature(Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "target.jpg", Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_PICTURES) + File.separator + "HodooOpenCV" + File.separator + "test.jpg");

//            if ( DEBUG ) Log.e(TAG, String.format("feature : %d", feature));
//
//            if ( feature > 0 ) {
//                if ( DEBUG ) Log.e(TAG, "Tow images are same.");
//            } else {
//                if ( DEBUG ) Log.e(TAG, "Tow images are different.");
//            }
//
//            if ( DEBUG ) return originalMat;


            Mat grayMat, resultMat, cannyMat, downMat, upMat, contourMat, hovIMG, tempMat;
            grayMat = cannyMat = downMat = upMat = hovIMG = tempMat = new Mat();

            int threshold = 100;
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            List<Mat> hlsChannels = new ArrayList<>();
            Mat lignt = new Mat();
            tempMat = new Mat();
            Imgproc.cvtColor(inputMat, tempMat, Imgproc.COLOR_RGB2HLS);
            Core.split(tempMat, hlsChannels);
            Core.add(hlsChannels.get(1), new Scalar(-30), lignt);
            hlsChannels.set(1, lignt);
            Core.merge(hlsChannels, tempMat);
            Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_HLS2RGB);
            Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_RGB2RGBA);
            Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_RGBA2RGB);
            tempMat.convertTo(tempMat, -1, 2, -100);

            Imgproc.cvtColor(tempMat, grayMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.pyrDown(grayMat, downMat, new Size(inputMat.cols() / 2, inputMat.rows() / 2));
            Imgproc.pyrUp(downMat, upMat, inputMat.size());
            Imgproc.Canny(upMat, cannyMat, 0, threshold);
            Imgproc.dilate(cannyMat, cannyMat, new Mat(), new Point(1, 1), 2);

            contourMat = cannyMat.clone();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(contourMat, contours, hovIMG, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint cnt = contours.get(i);
                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
                Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);
                int numberVertices = (int) approxCurve.total();

                if ( approxCurve.total() >= 4 && approxCurve.total() < 5  ) {
                    int topMargin = 0, bottomMargin = 0, height = inputMat.height(), rectHeight = 0;
                    double topY = 8000, bottomY = 0, topRightX = 0, topLeftX = 800, topLeftY = 0;
                    for (int j = 0; j < numberVertices; j++) {
                        Point point = approxCurve.toArray()[j];
//                        Imgproc.circle(originalMat, point, 50, new Scalar(255, 0, 0), 10);
//                        Imgproc.putText(originalMat, String.valueOf(j), point, Core.FONT_HERSHEY_SIMPLEX, 10, new Scalar(255, 0, 0), 3);
                        if ( topY > point.y )
                            topY = point.y;
                        if ( topRightX < point.x) {
                            topRightX = point.x;
                        }
                        if ( topLeftX > point.x ) {
                            topLeftX = point.x;
                            topLeftY = point.y;
                        }
                        if ( bottomY < point.y )
                            bottomY = point.y;
                    }

                    /* 각도 계산을 위한 포인트 계산 (s) */
                    Point leftBottom = approxCurve.toArray()[0];
                    Point rightBottom = approxCurve.toArray()[0];
                    Point top = approxCurve.toArray()[0];
                    Point temp = null;
//                    if ( numberVertices < 4 )
//                        return null;

                    Point t1 = new Point( 8000, 8000 );
                    Point t2 = new Point( 8000, 8000 );
                    Point b1 = new Point(0, 0);
                    Point b2 = new Point(0, 0);

                    Point tl, tr, bl, br;
                    for (int j = 0; j < numberVertices; j++) {
                        Point point = approxCurve.toArray()[j];
                        Point tempPoint = null;
                        if ( t1.y > point.y ) {
                            tempPoint = t1;
                            t1 = point;
                        }

                        if ( t2.y > point.y ) {
                            if ( t1 != point )
                                t2 = point;
                            else
                                t2 = tempPoint;
                        }



                        if ( b1.y < point.y ) {
                            tempPoint = b1;
                            b1 = point;
                        }

                        if ( b2.y < point.y ) {
                            if ( b1 != point )
                                b2 = point;
                            else
                                b2 = tempPoint;
                        }

                    }
                    if ( t2.x > t1.x ) {
                        tr = t2;
                        tl = t1;
                    } else {
                        tl = t2;
                        tr = t1;
                    }
                    if ( b2.x > b1.x ) {
                        br = b2;
                        bl = b1;
                    } else {
                        bl = b2;
                        br = b1;
                    }

                    if ( DEBUG ) Log.e(TAG, String.format("numberVertices : %d", numberVertices));
                    for (int j = 0; j < numberVertices - 1; j++) {
                        Point point = approxCurve.toArray()[j];
                        Point cPoint = approxCurve.toArray()[j + 1];
                        if ( temp == null )
                            temp = point;

                        if ( point.x > cPoint.x && point.y < cPoint.y ) {
                            leftBottom = cPoint;
                        }
                        if ( point.x < cPoint.x || point.y < cPoint.y ) {
                            rightBottom = cPoint;
                        }
                        if ( point.y > cPoint.y )
                            top = cPoint;
                        if ( DEBUG ) Log.e(TAG, String.format("position : %d, x : %f, y : %f, numberVertices : %d", j, point.x, point.y, numberVertices));

                    }
//                    startX = testLeftTop;
                    /* 각도 계산을 위한 포인트 계산 (e) */



                    /* 사각 프레임 기울기 계산 및 회전 적용 (s) */
                    Mat rotationMat = inputMat.clone();
                    double angle = getAngle(leftBottom, rightBottom);
                    if ( Math.abs(angle) > 10 )
                        angle = -0.7;
                    Mat rotation = Imgproc.getRotationMatrix2D(new Point(rotationMat.width() / 2, rotationMat.height() / 2), angle, 1);
                    Imgproc.warpAffine(rotationMat, rotationMat, rotation, new Size(rotationMat.cols(), rotationMat.rows()));
                    /* 사각 프레임 기울기 계산 및 회전 적용 (s) */


                    Imgproc.circle(originalMat, bl, 50, new Scalar(255, 0, 0), 20);
                    Imgproc.circle(originalMat, br, 50, new Scalar(0, 255, 0), 20);

                    Imgproc.circle(originalMat, tl, 50, new Scalar(0, 0, 255), 20);
                    Imgproc.circle(originalMat, tr, 50, new Scalar(255, 218, 185), 20);
//                    Imgproc.putText(originalMat, "left", testLeftTop, Core.FONT_HERSHEY_SIMPLEX, 5, new Scalar(0, 0, 255), 10);
//                    Imgproc.circle(originalMat, rightBottom, 50, new Scalar(255, 0, 0), 20);

//                    Imgproc.line(originalMat, leftBottom, rightBottom, new Scalar(0, 255, 0), 20);

//                    Point linePoint = new Point(originalMat.width() - leftBottom.x, leftBottom.y);
//                    Imgproc.line(originalMat, leftBottom, linePoint, new Scalar(0, 0, 255), 20);
                    double degree = getAngle(bl, br);
                    if ( DEBUG ) Log.e(TAG, String.format("degree : %f", degree));


                    int bottom;
                    if ( bl.y < br.y )
                        bottom = (int) br.y;
                    else
                        bottom = (int) bl.y;
                    if ( tl.y < tr.y )
                        topMargin = (int) tl.y;
                    else
                        topMargin = (int) tr.y;

                    bottomMargin = height - bottom;
                    rectHeight = (int) Math.abs(height - topMargin - bottomMargin);

//                    Point rectPoint1 = approxCurve.toArray()[1]; //왼쪽위
//                    Point rectPoint2 = approxCurve.toArray()[2]; //왼쪽아래
//
//                    int height = inputMat.height();
//                    double topMargin = rectPoint1.y;
//                    double bottomMargin = height - rectPoint2.y;
//                    double rectHeight = height - topMargin - bottomMargin;

                    Mat crop;
                    tempMat = new Mat();
                    //rotationMat.width()
                    Rect roi = new Rect(0, (int) topY, rotationMat.width(), rectHeight);
//                    if ( 0 <= roi.x && 0 <= roi.width && roi.x + roi.width <= inputMat.cols() && 0 <= roi.y && 0 <= roi.height && roi.y + roi.height <= inputMat.rows() ) {
                        crop = rotationMat.submat(roi);
                        tempMat = crop.clone();
                        break;
//                    }
                }
            }


//            if ( DEBUG ) return originalMat;
            Mat roi = tempMat.clone();
            Mat cloneMat = tempMat.clone();
            resultMat = cloneMat.clone();


//            if ( DEBUG ) return resultMat;
//            if ( DEBUG ) Log.e(TAG, String.format("roi.height() : %d", roi.height()));
            if ( roi.height() < 150 )
                return null;

            Imgproc.cvtColor(tempMat, roi, Imgproc.COLOR_RGB2HSV);
            Imgproc.erode(tempMat, tempMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(2, 2)), new Point(-1, -1), 3);
            Core.inRange(roi, new Scalar(15, 15, 15), new Scalar(200, 255, 255), tempMat);
//            if ( DEBUG ) return tempMat;
            Imgproc.Canny(tempMat, tempMat, 50, 255);
            Imgproc.dilate(tempMat, tempMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(4, 4)), new Point(-1, -1), 3);
            Imgproc.erode(tempMat, tempMat, Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(2, 2)), new Point(-1, -1), 3);



            contours = new ArrayList<>();
            approxCurve = new MatOfPoint2f();
            Imgproc.findContours(tempMat, contours, hovIMG, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            List<Integer> widthList = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();
            for ( int i = 0; i < contours.size(); i++ ) {
                MatOfPoint cnt = contours.get(i);
                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
                Rect rect = Imgproc.boundingRect(cnt);
                if (rect.width < 30) continue;
                Imgproc.approxPolyDP(curve, approxCurve, 0.04 * Imgproc.arcLength(curve, true), true); //다각형 검출
                if ( approxCurve.total() >= 4 ) {
                    if ( rect.width >= 300 && rect.width < 400) {
                        rects.add(rect);
                    }
                }
            }
            //bubble sort
            for ( int i = 0; i < rects.size() - 1; i++ ) {
                for ( int j = i + 1; j < rects.size(); j++ ) {
                    if ( rects.get(i).x > rects.get(j).x ) {
                        Rect tempRect = rects.get(i);
                        rects.set(i, rects.get(j));
                        rects.set(j, tempRect);
                    }
                }
            }
            List<Rect> newRects = new ArrayList<>();
            for (int i = 0; i < rects.size(); i++) {
                if ( i > 0 ) {
                    if ( rects.get(i - 1) != null ) {
                        if (rects.get(i).x - rects.get(i - 1).x < 100) {
                            continue;
                        }
                    }
                }
                newRects.add(rects.get(i));

////                Log.e(TAG, String.format("rects.get(i).x : %d", rects.get(i + 1).x - (rects.get(i).x + rects.get(i).width) ));
            }
            rects = newRects;
//            Log.e(TAG, String.format("rects.get(0).x : %d , rect width : %d", rects.get(0).x, rects.get(0).width));
//
            /* 빈곳 메우기 (첫번째가 없는 경우) */
//            if ( rects.get(0).x > 200 ) {
////                imagePosition = ComburResult.COMBUR_ROTATION_180;
//                int rightMargin = 0, width = 0, x1 = 0, x2 = 0, startX = 0;
//                x1 = rects.get(0).x;
//                x2 = rects.get(1).x;
//                width = rects.get(0).width;
//                rightMargin = x2 - x1 - width;
//                startX = x1 - rightMargin - width;
//                Point firstPoint1 = new Point( startX, rects.get(0).y);
//                Point firstPoint2 = new Point(startX + width, rects.get(0).y + rects.get(0).height);
//                Rect firstRect = new Rect(firstPoint1, firstPoint2);
//                rects.add(0, firstRect);
//            }
//            for (int i = 0; i < rects.size(); i++) {
//                if ( i > 0 ) {
//                    Log.e(TAG, String.format("rects.get(i).x : %d", rects.get(i + 1).x - (rects.get(i).x + rects.get(i).width) ));
//
//                    if ( rects.get(i - 1) != null ) {
//                        if (rects.get(i).x - rects.get(i - 1).x < 100) {
//                            if (DEBUG)
//                                Log.e(TAG, String.format("compare (%d) : %d", i, rects.get(i).x - rects.get(i - 1).x));
//                            rects.set(i, null);
//                            continue;
//                        }
//                    }
//                }
//                Point p1 = new Point(rects.get(i).x, rects.get(i).y);
//                Point p2 = new Point(rects.get(i).x + rects.get(i).width , rects.get(i).y + rects.get(i).height);
//                Imgproc.rectangle(resultMat, p1, p2, new Scalar(0, 0, 255), 10);
//
////                Log.e(TAG, String.format("rects.get(i).x : %d", rects.get(i + 1).x - (rects.get(i).x + rects.get(i).width) ));
//            }
            for (int i = 0; i < rects.size(); i++) {
                if ( rects.get(i) == null )
                    rects.remove(i);
            }
//            if ( DEBUG ) return resultMat;

            int litmusWidth = 0, startPoint = 0, litmusMargin = 0, totalWidth = 0, totalHeight = 0, totalMargin = 0, tempX = 0, count = 0, startY = 0, litmusHeight = 0, litmusSpacing = 0;
            for ( int i = 0; i < rects.size(); i++ ) {
                if ( DEBUG ) Log.e(TAG, String.format("rect w : %d, rect x : %d", rects.get(i).width, rects.get(i).x));
                if ( i == 0 )
                    startPoint = tempX = rects.get(i).x;
                else {
                    if ( rects.get(i).x - rects.get(i -1).x < 10 )
                        continue;
                    if ( litmusMargin != 0 && rects.get(i).x - rects.get(i -1).x > litmusMargin * 1.5 )
                        continue;
                    if ( ( rects.get(i).x - (rects.get(i -1).x + rects.get(i - 1).width  )) < 0 )
                        continue;
                    litmusMargin = rects.get(i).x - (rects.get(i -1).x + rects.get(i - 1).width);
                    totalMargin += litmusMargin;
                    totalWidth += rects.get(i).width;
                    totalHeight += rects.get(i).height;
                    if ( startY == 0 )
                        startY = rects.get(i).y;
                    else if ( startY != 0 && startY > rects.get(i).y )
                        startY = rects.get(i).y;
                    count++;
                }
            }
            if ( DEBUG ) Log.e(TAG, String.format("litmusMargin : %d, startX : %d", litmusMargin, startPoint));
            int firstX = 180, firstW = 325, detectPointX = (firstX + firstW) / 2, abs;

            if ( rects.get(0).x - firstX > 0 )
                abs = rects.get(0).x - firstX;
            else
                abs = firstX - rects.get(0).x;
            if ( abs > 100 ) {
                Log.e(TAG, "first rect over one hundred");
                Rect rect = new Rect(firstX, startY, firstW, litmusHeight);
                rects.add(0, rect);
            }

            if ( totalWidth == 0 || totalHeight == 0 || count == 0 )
                return null;
            litmusWidth = totalWidth / count;
            litmusHeight = totalHeight / count;


//            if ( DEBUG ) Log.e(TAG, String.format("startPoint : %d,first x : %d, first width : %d", startPoint, rects.get(0).x, rects.get(0).width));
//            int firstX = 180, firstW = 325, detectPointX = (firstX + firstW) / 2;
//            int initStart = 150, x = initStart + detectPointX;
//            int y = startY + (litmusHeight / 2);
//            float initHue = 70, abs = 0;
//
//            Point test1 = new Point(x, y);
//            Imgproc.circle(resultMat, test1, 30, new Scalar(255, 180, 180), 20);
//
//            double R=0,G=0,B=0;
//            double[] color = cloneMat.get(y, x);
//            boolean rotationState = false;
//            if ( color != null ) {
//                R = color[0];
//                G = color[1];
//                B = color[2];
//                float[] hsv = new float[3];
//                Color.RGBToHSV((int) R, (int) G, (int) B, hsv);
//                if ( initHue - hsv[0] > 0 )
//                    abs = initHue - hsv[0];
//                else
//                    abs = hsv[0] - initHue;
//                if ( !(abs > 10) && !(abs < -10) )
//                    Log.e(TAG, "10도 이내");
//                else {
//                    Log.e(TAG, "10도 이상");
//                    Rect firstRect = new Rect(firstX, startY, firstW, litmusHeight);
//                    rects.add(0, firstRect);
//                    startPoint = firstRect.x;
//                    litmusHeight = litmusHeight - 100;
//
//                    Mat rotation = Imgproc.getRotationMatrix2D(new Point(resultMat.width() / 2, resultMat.height() / 2), 180, 1);
//                    Imgproc.warpAffine(resultMat, resultMat, rotation, new Size(resultMat.cols(), resultMat.rows()));
//                    rotationState = true;
//                }
////                if ( initHue )
//                if ( DEBUG ) Log.e(TAG, String.format("h : %f, s : %f, v : %f", hsv[0], hsv[1] * 100, hsv[2] * 100));
//            }

            startPoint = 180;
            litmusMargin = 140;
            litmusWidth = 320;

            for ( int i = 0; i < litmusBoxNum; i++ ) {
                Point a = new Point(startPoint + litmusSpacing, startY);
                Point b = new Point( startPoint + litmusSpacing + litmusWidth, startY + litmusHeight );
                Imgproc.rectangle(resultMat, a, b, new Scalar(0, 0, 255), 5);
                int x = (int) (a.x + b.x) /  2;
                int y = (int) (a.y + b.y) /  2;

                double R=0, G=0, B=0;
                double[] color = cloneMat.get(y, x);

                if ( color != null ) {
                    R = color[0];
                    G = color[1];
                    B = color[2];
                    float[] hsv = new float[3];
                    Color.RGBToHSV((int) R, (int) G, (int) B, hsv);
                    HodooFindColor findColor = HodooFindColor.builder().red((int) R).green((int) G).blue((int) B).index(i + 1).hsv(hsv).build();
                    colors.add(findColor);
                }


                Point point = new Point(x, y);
                Imgproc.circle(resultMat, point, 5, new Scalar(0, 0, 255), 5);
                litmusSpacing += litmusMargin + litmusWidth;
            }
//            if ( rotationState ) {
//                Mat rotation = Imgproc.getRotationMatrix2D(new Point(resultMat.width() / 2, resultMat.height() / 2), 180, 1);
//                Imgproc.warpAffine(resultMat, resultMat, rotation, new Size(resultMat.cols(), resultMat.rows()));
//            }


            return resultMat;
        }

        @Override
        protected void onPostExecute(Mat mat) {
            super.onPostExecute(mat);
            mView.setProgressLayout(View.GONE);
            mView.setColorList(colors);
            setImg(mat);
        }
    }
    @Override
    public void setImg(Mat inputMat) {
        if ( inputMat == null ) {
            inputMat = originalMat.clone();
            mView.toast("사각형이 검출되지 않았습니다.\n 사진을 다시 찍어주세요.");
        }
        mView.setImage( convertMatToBitmap(inputMat) );
    }

    @Override
    public void requestRetrofit(Context context, List<HodooFindColor> colors) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            float[] hsv = colors.get(i).getHsv();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < hsv.length; j++) {
                if ( j == 0 )
                    sb.append(String.format("%.0f", Math.floor(Math.abs(colors.get(i).getHsv()[j]))));
                else
                    sb.append(String.format("%.0f", Math.floor(Math.abs(colors.get(i).getHsv()[j] * 100))));
                if ( j != hsv.length - 1 )
                    sb.append("/");
            }
            values.add(sb.toString());
        }
        if ( values.size() >= 11 ) {
            HsvValue hsvValue = new HsvValue();
            hsvValue.setSg(values.get(0));
            hsvValue.setPh(values.get(1));
            hsvValue.setLeu(values.get(2));
            hsvValue.setNit(values.get(3));
            hsvValue.setPro(values.get(4));
            hsvValue.setGlu(values.get(5));
            hsvValue.setKet(values.get(6));
            hsvValue.setUbg(values.get(7));
            hsvValue.setBil(values.get(8));
            hsvValue.setEry(values.get(9));
            hsvValue.setHb(values.get(10));
            NetworkAsync async = new NetworkAsync(context);
            async.execute(hsvValue);
        }
    }

    private Mat readMatImg ( String path ) {
        Mat readMat = Imgcodecs.imread(path);
        return readMat;
    }
    private Bitmap convertMatToBitmap ( Mat bitmapMat ) {
        Bitmap bitmap = Bitmap.createBitmap( bitmapMat.cols(), bitmapMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(bitmapMat, bitmap);
        return bitmap;
    }
    private Mat setHLS( Mat inputMat, int... hls ) {
        int max = hls.length;
        Mat resultMat = new Mat();

        List<Mat> hlsChannels = new ArrayList<>();
        Mat[] mats = new Mat[max];
        Imgproc.cvtColor(inputMat, resultMat, Imgproc.COLOR_RGB2HLS);
        Core.split(resultMat, hlsChannels);
        for (int i = 0; i < max; i++) {
            Core.add(hlsChannels.get(i), new Scalar(hls[i]), mats[i]);
            hlsChannels.set(i, mats[i]);
        }
        Core.merge(hlsChannels, resultMat);
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_HLS2RGB);
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2RGBA);
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGBA2RGB);
        return resultMat;
    }
    public class NetworkAsync extends AsyncTask<HsvValue, Void, HsvValue> {
        private Context mContext;
        NetworkAsync ( Context context ) {
            mContext = context;
        }

        @Override
        protected HsvValue doInBackground(HsvValue... hsvValues) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(mContext.getString(R.string.base_url))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            RetrofitService service = retrofit.create(RetrofitService.class);
            HsvValue hsv = hsvValues[0];
            Call<HsvValue> requrest = service.getHsv(hsv);
            requrest.enqueue(new Callback<HsvValue>() {
                @Override
                public void onResponse(Call<HsvValue> call, Response<HsvValue> response) {
                    HsvValue hsv = response.body();

                    if ( hsv != null ) {
                        int[] msgResource = {
                                R.array.sg_str_arr,
                                R.array.ph_str_arr,
                                R.array.leu_str_arr,
                                R.array.nit_str_arr,
                                R.array.pro_str_arr,
                                R.array.glu_str_arr,
                                R.array.ket_str_arr,
                                R.array.ubg_str_arr,
                                R.array.bil_str_arr,
                                R.array.ery_str_arr,
                                R.array.hb_str_arr
                        };
                        String[] name = {
                                "SG",
                                "pH",
                                "LEU",
                                "NIT",
                                "PRO",
                                "GLU",
                                "KET",
                                "UBG",
                                "BIL",
                                "ERY",
                                "Hb"
                        };
                        List<ComburResult> results = new ArrayList<>();
                        String[] arr = hsv.toArray();
                        for (int i = 0; i < arr.length; i++) {
                            int index = Integer.parseInt(arr[i]);
                            int position = 0;
                            int count = 0;
                            String[] msg = mContext.getResources().getStringArray(msgResource[i]);
                            if ( i == 0 ) {
                                if ( index < 2 )
                                    position = 0;
                                else if ( index >= 2 && index < 5 )
                                    position = 1;
                                else
                                    position = 2;
                            } else if ( i == 1 ) {
                                if ( index < 1 )
                                    position = 0;
                                else if ( index >= 1 && index < 4 )
                                    position = 1;
                                else
                                    position = 2;
                            } else if ( i == 3 ) {
                                if ( index < 1 )
                                    position = 0;
                                else
                                    position = 1;
                            }  else {
                                if ( index < 2 )
                                    position = 0;
                                else if ( index == 2 )
                                    position = 1;
                                else
                                    position = 2;
                            }
                            ComburResult result = ComburResult.builder()
                                    .comburTitle(name[i])
                                    .resultMsg(msg[position])
                                    .position(i)
                                    .resultPosition( Integer.parseInt(arr[i]) )
                                    .imgPreStr(name[i].toLowerCase())
                                    .resultPosition(Integer.parseInt(arr[i]))
                                    .build();
                            result.imgSetting(mContext);
                            results.add(result);
                        }
                        mView.setCombur(results);
                    } else {
                        Log.e(TAG, "server error");
                    }

                }

                @Override
                public void onFailure(Call<HsvValue> call, Throwable t) {

                }
            });
            try {
                requrest.clone().execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(HsvValue value) {
            super.onPostExecute(value);
        }
    }

    public double getAngle(Point start, Point end) {
        int dx = (int) (end.x - start.x);
        int dy = (int) (end.y - start.y);

        double rad= Math.atan2(dx, dy);
        double degree = (rad*180)/Math.PI;
        return 90 - degree;
    }
}
