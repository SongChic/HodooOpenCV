package com.ahqlab.hodooopencv.activity.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ahqlab.hodooopencv.view.CameraPreview;

import org.opencv.core.Point;

import java.util.List;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;

public class BasicDrawer extends View {

    private final static String TAG = BasicDrawer.class.getSimpleName();
    
    RectDrawer drawer = null;
    Point mPoint;
    double mX1;

    public BasicDrawer(Context context) {
        this(context, null);
    }

    public BasicDrawer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasicDrawer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        drawer = new RectDrawer(this);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawer.draw(canvas);
    }
    public void setPoint (List<Point> point) {
        drawer.setPoint(point);
    }
    public float getScale() {
        return Math.min(CameraPreview.mWidth / 1920, CameraPreview.mHeight / 1620);
    }
}