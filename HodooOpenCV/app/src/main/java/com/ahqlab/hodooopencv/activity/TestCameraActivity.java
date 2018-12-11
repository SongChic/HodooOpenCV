package com.ahqlab.hodooopencv.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.activity.draw.BasicDrawer;
import com.ahqlab.hodooopencv.base.BaseActivity;
import com.ahqlab.hodooopencv.databinding.TestCameraActivityBinding;
import com.ahqlab.hodooopencv.domain.HodooWrapping;
import com.ahqlab.hodooopencv.presenter.HodooCameraPresenterImpl;
import com.ahqlab.hodooopencv.presenter.interfaces.HodooCameraPresenter;
import com.ahqlab.hodooopencv.view.CameraPreview;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;

public class TestCameraActivity extends BaseActivity<TestCameraActivity> implements HodooCameraPresenter.VIew {
    TestCameraActivityBinding binding;

    private HodooWrapping mWrapping;
    private HodooCameraPresenter.Precenter mPrecenter;

    CameraPreview mCameraPreview;
    BasicDrawer mBasicDrawer;
    private int mDeviceWidth;
    private int mDeviceHeight;
    private List<Point> mPoints;

    private Bitmap warppingResult;
    private boolean mBlurState = false;

    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = DataBindingUtil.setContentView(this, R.layout.test_camera_activity);
        binding.setActivity(this);
        mBasicDrawer = new BasicDrawer(this);
        mPrecenter = new HodooCameraPresenterImpl(this);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        if ( DEBUG ) Log.e(TAG, String.format("device width : %d, height : %d", width, height));

        addContentView(mBasicDrawer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    private void startCamera () {
        mCameraPreview = new CameraPreview(this, this, CAMERA_FACING, binding.cameraPreview);
    }
    private void stopCamera() {
        mCameraPreview.stopCamera();
    }
    public void setMap (Mat resultMat) {
        Bitmap bitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(resultMat, bitmap);

//        Matrix matrix = new Matrix();
//        matrix.postRotate(90);
//
//        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//        bitmap.recycle();

//        binding.imgPreview.setImageBitmap(bitmap);
    }
    public void updateView () {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
            mBasicDrawer.postInvalidate();
        else
            mBasicDrawer.invalidate();
    }
    public void setPoint (List<Point> point) {
        mBasicDrawer.setPoint(point);
        mPoints = point;
        mWrapping = HodooWrapping.builder().points(point).build();
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    public void onTakePictureClick (View v) {
        mCameraPreview.takePicture(getString(R.string.app_name), "/test.jpg", new CameraPreview.CameraCallback() {
            @Override
            public void onResult(String fileName) {
                mWrapping.setFileName(fileName);
                mWrapping.setTr(mPoints.get(0));
                mWrapping.setTl(mPoints.get(1));
                mWrapping.setBl(mPoints.get(2));
                mWrapping.setBr(mPoints.get(3));
                mPrecenter.wrappingProcess(mWrapping);
            }
        });
    }

    @Override
    public void setWrappingImg(Bitmap resultMat) {
        Log.e(TAG, "setWrappingImg start");

        final Dialog dialog = new Dialog(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.camera_alert_layout, null);
        dialog.setContentView(v);
        dialog.setCancelable(false);
        dialog.show();

        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point point = new android.graphics.Point();
        display.getSize(point);

        int width = (int) (point.x * 0.8f);
        int height = (int) (point.y * 0.7f);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBlurState = false;
            }
        });
        final ImageView pictureView = dialog.findViewById(R.id.progress_img);
        pictureView.setAdjustViewBounds(true);

        Button usePicture = v.findViewById(R.id.use_picture);
        usePicture.setText(R.string.camera_confirm);
        usePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( warppingResult != null ) {
                    mPrecenter.saveWrappingImg(TestCameraActivity.this, warppingResult);
                    dialog.dismiss();
                }
            }
        });
        Button retry = v.findViewById(R.id.retry);
        retry.setText(R.string.camera_cancel);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                pictureView.setImageBitmap(null);
            }
        });
        usePicture.requestLayout();
        retry.requestLayout();

        pictureView.setImageBitmap(resultMat);
        warppingResult = resultMat;
        mBlurState = true;
        Log.e(TAG, "setWrappingImg end");
    }

    @Override
    public void saveImgResult(boolean state, String path) {
        if ( state ) {
            Intent intent = new Intent(TestCameraActivity.this, AnalysisActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
//            if ( mImageView != null ) {
//                binding.maskView.setVisibility(View.GONE);
//                mImageView = null;
//            }
            mBlurState = false;
        } else {
            Toast.makeText(this, "사진 저장에 실패했습니다.\n잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void setPresenter(HodooCameraPresenter.Precenter presenter) {

    }
}
