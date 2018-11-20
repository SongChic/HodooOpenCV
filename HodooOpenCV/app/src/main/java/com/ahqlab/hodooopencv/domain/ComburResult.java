package com.ahqlab.hodooopencv.domain;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.ahqlab.hodooopencv.R;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ComburResult {
    private String comburTitle;
    private int position;
    private int resultPosition;
    private String resultMsg;
    private String detectColor;
    private final int[] imgs = new int[11];
    private final int[] maxNum = {
      7, 5, 4, 2, 4, 5, 4, 5, 4, 5, 5
    };
    private final String imgPreStr;
    public void imgSetting (Context context) {
        for (int i = 0; i < maxNum[position]; i++) {
            imgs[i] = context.getResources().getIdentifier(imgPreStr + "_" + String.format("%02d", i), "drawable", context.getPackageName());
        }
    }
}
