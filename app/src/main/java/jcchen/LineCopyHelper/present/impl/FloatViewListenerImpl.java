package jcchen.LineCopyHelper.present.impl;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import jcchen.LineCopyHelper.R;
import jcchen.LineCopyHelper.present.FloatViewListener;
import jcchen.LineCopyHelper.view.FloatView;
import jcchen.LineCopyHelper.view.RoundedImageView;

/**
 * Created by JCChen on 2018/5/6.
 */

public class FloatViewListenerImpl implements FloatViewListener {
    private static final String TAG = FloatViewListenerImpl.class.getName();

    private Context context;
    private RoundedImageView icon_RImageView, background_RImageView;

    public FloatViewListenerImpl(Context context, FloatView floatView) {
        this.context = context;
        icon_RImageView = (RoundedImageView) floatView.findViewById(R.id.icon_RImageView);
        background_RImageView = (RoundedImageView) floatView.findViewById(R.id.background_RImageView);
    }

    @Override
    public void onSwitchOn() {
        icon_RImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_stop));
        background_RImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorFloatViewStop));
        Toast.makeText(context, "Start", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSwitchOff() {
        icon_RImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_start));
        background_RImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorFloatViewStart));
        Toast.makeText(context, "Stop", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        context = null;
        icon_RImageView = null;
        background_RImageView = null;
    }
}
