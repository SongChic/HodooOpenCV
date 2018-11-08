package com.ahqlab.hodooopencv.base;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity<D extends Activity> extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();
    public Activity setActivity () {
        return this;
    }
}
