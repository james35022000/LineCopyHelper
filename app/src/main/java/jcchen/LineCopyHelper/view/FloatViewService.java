package jcchen.LineCopyHelper.view;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import jcchen.LineCopyHelper.R;
import jcchen.LineCopyHelper.present.impl.FloatViewListenerImpl;
import jcchen.LineCopyHelper.present.impl.FloatViewPresenterImpl;

/**
 * Created by JCChen on 2018/5/5.
 */

public class FloatViewService extends Service {
    private static final String TAG = FloatViewService.class.getName();

    private FloatView floatView;
    private RelativeLayout icon_relativeLayout;
    private WindowManager windowManager;
    private FloatViewPresenterImpl floatViewPresenter;
    private FloatViewListenerImpl floatViewListener;

    @Override
    public void onCreate() {
        super.onCreate();
        floatView = (FloatView) LayoutInflater.from(getBaseContext()).inflate(R.layout.float_view, null);
        icon_relativeLayout = (RelativeLayout) floatView.findViewById(R.id.icon_relativeLayout);
        floatViewPresenter = new FloatViewPresenterImpl(this, floatView);
        floatViewListener = new FloatViewListenerImpl(this, floatView);

        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Click");
                if(floatViewPresenter.getExitState())
                    floatViewPresenter.exit();
                else
                    floatViewPresenter.serviceSwitch(floatViewListener);
            }
        });

        floatView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "LongClick");
                if(floatViewPresenter.getExitState())
                    return false;
                floatViewPresenter.showExitOption();
                return true;
            }
        });


        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = floatView.getLayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_PHONE;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.START | Gravity.TOP;
        params.x += 20;
        params.y += 20;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.RGBA_8888;

        windowManager.addView(floatView, params);
        Log.d(TAG, "Add FloatView to window");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        windowManager.removeView(floatView);
        floatView = null;
        icon_relativeLayout = null;
        windowManager = null;
        floatViewPresenter.onDestroy();
        floatViewPresenter = null;
        floatViewListener.onDestroy();
        floatViewListener = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
