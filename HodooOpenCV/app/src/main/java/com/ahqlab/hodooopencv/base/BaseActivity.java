package com.ahqlab.hodooopencv.base;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import static com.ahqlab.hodooopencv.constant.HodooConstant.SHARED_NAME;

public abstract class BaseActivity<D extends Activity> extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();
    public Activity setActivity () {
        return this;
    }
    public SharedPreferences getPreferences(int mode){
        SharedPreferences pref = getSharedPreferences(SHARED_NAME, mode);
        return pref;
    }
}
