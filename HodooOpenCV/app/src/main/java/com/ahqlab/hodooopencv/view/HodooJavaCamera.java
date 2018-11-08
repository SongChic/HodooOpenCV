package com.ahqlab.hodooopencv.view;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.FileOutputStream;

public class HodooJavaCamera extends JavaCameraView implements Camera.PictureCallback {
    public interface CameraCallback {
        void onResult( String fileName );
    }

    private final String TAG = HodooJavaCamera.class.getSimpleName();
    private String mPictureFileName;
    private String mFolerName;
    private CameraCallback mCameraCallback;
    public HodooJavaCamera(Context context, int cameraId) {
        super(context, cameraId);
    }

    public HodooJavaCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        File folder = new File(mFolerName);
        if ( !folder.isDirectory() ) {
            folder.mkdirs(); //폴더 생성
        }


        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mFolerName + mPictureFileName);

            fos.write(data);
            fos.close();
            File file = new File(mFolerName + mPictureFileName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                final Uri contentUri = Uri.fromFile(file);
                scanIntent.setData(contentUri);
                getContext().sendBroadcast(scanIntent);
            } else {
                final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
                getContext().sendBroadcast(intent);
            }
            if ( mCameraCallback != null )
                mCameraCallback.onResult(file.getAbsolutePath());
//            getContext().sendBroadcast( new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        } finally {
            mCameraCallback = null;
        }
    }
    public void takePicture(String folder, final String fileName, CameraCallback callback) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        mCameraCallback = callback;
        mFolerName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + folder;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }
}
