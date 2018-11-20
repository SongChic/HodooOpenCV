package com.ahqlab.hodooopencv.presenter.interfaces;

import android.content.Context;
import android.graphics.Bitmap;

import com.ahqlab.hodooopencv.base.BasePresenter;
import com.ahqlab.hodooopencv.base.BaseView;
import com.ahqlab.hodooopencv.domain.ComburResult;
import com.ahqlab.hodooopencv.domain.HodooFindColor;
import com.ahqlab.hodooopencv.domain.HsvValue;

import org.opencv.core.Mat;

import java.util.List;

public interface AnalysisPresenter {
    interface VIew extends BaseView<Precenter> {
        void setImage(Bitmap img);
        void setProgressLayout( int state );
        void setColorList(List<HodooFindColor> colors);
        void toast( String msg );
        void setCombur (List<ComburResult> results);
    }

    interface Precenter extends BasePresenter {
        void imageProcessing( String path );
        void setImg( Mat inputMat );
        void requestRetrofit(Context context,  List<HodooFindColor> colors);
    }
}
