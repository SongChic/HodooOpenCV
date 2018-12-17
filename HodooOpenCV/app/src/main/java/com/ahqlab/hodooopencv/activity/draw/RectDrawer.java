package com.ahqlab.hodooopencv.activity.draw;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

import static com.ahqlab.hodooopencv.constant.HodooConstant.DEBUG;

public class RectDrawer {
    private static final String TAG = RectDrawer.class.getSimpleName();
    private BasicDrawer mBasicDrawer;
    List<Point> mPoint;

    private boolean focusState = false;
    private boolean focusSuccessState = true;
    private float x;
    private float y;

    public RectDrawer ( BasicDrawer basicDrawer ) {
        mBasicDrawer = basicDrawer;
    }
    public void setPoint ( List<Point> point ) {
        mPoint = point;
    }
    public void setFocusPoint (float x, float y) {
        this.x = x;
        this.y = y;
        this.focusState = true;
        focusSuccessState = true;
    }
    public void setFocusState ( boolean state ) {
        focusSuccessState = state;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                focusState = false;
            }
        }, 3000);
    }
    public void draw (Canvas canvas) {
        if (focusState) {
            Paint paint = new Paint();
            if (focusSuccessState)
                paint.setColor(Color.GREEN);
            else
                paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            canvas.drawRect(x - 150, y - 150, x + 150, y + 150, paint);
        }
        if ( mPoint != null && mPoint.size() > 0 ) {
            Paint paint = new Paint();
            paint.setColor(Color.CYAN);
            paint.setAlpha(127);


            double scale = Math.min((double)2560/1920, (double) 1440/1080);
            
            Log.e(TAG, String.format("math min : %f", Math.min((double)2560/1920, (double) 1440/1080)));




            double xoffset = ((double)1440-scale*(double)1080)/2;
            double yoffset = ((double)2560-scale*(double)1920)/2 - 40;

            if ( DEBUG ) Log.e(TAG, String.format("xoffset : %f, yoffset : %f, scale : %f", xoffset, yoffset, scale));

//            Point leftTop = new Point(1440, 2560), rightTop = new Point(0, 2560), leftBottom = new Point(1440, 0), rightBottom = new Point(0, 0);
            float startX = (float) (mPoint.get(0).x*scale+xoffset), startY = (float) (mPoint.get(0).y*scale+yoffset);
            Path path = new Path();
            path.moveTo(startX, startY);
            for (int i = 0; i < mPoint.size(); i++) {
                path.lineTo((float) (mPoint.get(i).x*scale+xoffset), (float) (mPoint.get(i).y*scale+yoffset));
            }
            path.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if ( path.isConvex() ) canvas.drawPath(path, paint);
            }
        }
    }
}
