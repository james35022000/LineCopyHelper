package jcchen.floatview.model;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import jcchen.floatview.present.SwitchService;

/**
 * Created by JCChen on 2018/5/5.
 */

public class LineCopyService extends AccessibilityService {
    private static final String TAG = LineCopyService.class.getName();

    private ClipboardManager clipboardManager = null;
    private ClipData clipData;
    private SwitchService switchService;
    private boolean isBind = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "Monitoring!");
        if(!isMyServiceRunning(SwitchService.class))  return;
        if(!isBind) {
            Intent intent = new Intent(this, SwitchService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            return;
        }
        if(switchService.getMode() == SwitchService.MODE_OFF)  return;
        if(switchService.getMode() == SwitchService.MODE_EXIT) {
            unbindService(mConnection);
            switchService = null;
            isBind = false;
            return;
        }

        if(clipboardManager == null) {
            clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        }
        int type=event.getEventType();
        switch (type){
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.i(TAG, event.getClassName().toString());
                if(event.getClassName().toString().equals("android.widget.TextView")) {
                    Log.d(TAG, "Copy to clipboard");
                    clipData = ClipData.newPlainText("", event.getSource().getText());
                    clipboardManager.setPrimaryClip(clipData);
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                break;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Connected!" + this.toString());
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        clipboardManager = null;
        clipData = null;
        switchService = null;
        mConnection = null;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SwitchService.SwitchBinder binder = (SwitchService.SwitchBinder) service;
            switchService = binder.getService();
            isBind = true;
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            switchService = null;
            isBind = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
