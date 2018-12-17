package com.ahqlab.hodooopencv.activity;

import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.adapter.ColorListAdapter;
import com.ahqlab.hodooopencv.adapter.ComburListAdapter;
import com.ahqlab.hodooopencv.base.BaseActivity;
import com.ahqlab.hodooopencv.databinding.ActivityAnalsisBinding;
import com.ahqlab.hodooopencv.domain.ComburResult;
import com.ahqlab.hodooopencv.domain.HodooFindColor;
import com.ahqlab.hodooopencv.domain.HsvValue;
import com.ahqlab.hodooopencv.presenter.AnalysisPresenterImpl;
import com.ahqlab.hodooopencv.presenter.interfaces.AnalysisPresenter;
import com.ahqlab.hodooopencv.util.HodooUtil;

import org.opencv.core.Mat;

import java.io.File;
import java.util.List;

public class AnalysisActivity extends BaseActivity<AnalysisActivity> implements AnalysisPresenter.VIew {
    private ActivityAnalsisBinding binding;
    private List<HodooFindColor> colors;
    private Mat inputImg, mTransMat;
    private int litmusBoxNum = 11;
    private AnalysisPresenterImpl presenter;
    private Bitmap displayImg;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_analsis);
        String path = getIntent().getStringExtra("path");
        if ( path == null || path.equals("")) {
            path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ) + File.separator + getString( R.string.app_name ) + File.separator + "test.jpg";
        }
        presenter = new AnalysisPresenterImpl(this);
        presenter.imageProcessing(this, path);


    }

    @Override
    public void setImage(Bitmap img) {
        binding.resultImg.setImageBitmap(img);
        displayImg = img;
    }

    @Override
    public void setProgressLayout(int state) {
        binding.progressWrap.setVisibility(state);
    }

    @Override
    public void setColorList(List<HodooFindColor> colors) {
        if ( colors.size() > 0 ) {
            ColorListAdapter adapter = new ColorListAdapter(this, colors);
            binding.colorList.setAdapter(adapter);
            presenter.requestRetrofit(this, colors);
        }
    }

    @Override
    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setCombur(List<ComburResult> results) {
        binding.comburList.setItem(results);

//        ComburListAdapter adapter = new ComburListAdapter(this, results);
//        binding.comburList.setAdapter(adapter);


    }

    @Override
    public void setProgressUpdate(double value) {

    }

    @Override
    public void setPresenter(AnalysisPresenter.Precenter presenter) {

    }

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    @Override
    protected BaseActivity<AnalysisActivity> getActivityClass() {
        return null;
    }
}
