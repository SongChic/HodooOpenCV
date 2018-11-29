package com.ahqlab.hodooopencv.activity;

import android.content.SharedPreferences;

import com.ahqlab.hodooopencv.base.BaseActivity;
import com.ahqlab.hodooopencv.constant.HodooConstant;

public class Splash extends BaseActivity<Splash> {
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        int autoProcess = pref.getInt(HodooConstant.AUTO_PROCESS_KEY, 0);

    }
}
