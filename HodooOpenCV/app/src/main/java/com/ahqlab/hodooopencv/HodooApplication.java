package com.ahqlab.hodooopencv;

import android.app.Application;

public class HodooApplication extends Application {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
}
