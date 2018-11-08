package com.ahqlab.hodooopencv.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ahqlab.hodooopencv.R;
import com.ahqlab.hodooopencv.domain.HodooFindColor;

import java.util.List;

public class ColorListAdapter extends BaseAdapter {
    private Context mContext;
    private List<HodooFindColor> mColors;
    private LayoutInflater mInflater;

    public ColorListAdapter ( Context context, List<HodooFindColor> colors ) {
        mContext = context;
        mColors = colors;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return mColors.size();
    }

    @Override
    public Object getItem(int position) {
        return mColors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if ( convertView == null ) {
            convertView = mInflater.inflate(R.layout.item_color, parent, false);
            holder = new ViewHolder();
            holder.colorBox = convertView.findViewById(R.id.color_box);
            holder.index = convertView.findViewById(R.id.index);
            holder.colorCode = convertView.findViewById(R.id.color_code);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String hex = String.format("#%02x%02x%02x", mColors.get(position).getRed(), mColors.get(position).getGreen(),mColors.get(position).getBlue());
        holder.colorBox.setBackgroundColor(Color.parseColor(hex) );
        holder.index.setText( String.valueOf(mColors.get(position).getIndex()) );
        holder.colorCode.setText(hex);
        return convertView;
    }
    private class ViewHolder {
        private View colorBox;
        private TextView index;
        private TextView colorCode;
    }
}
