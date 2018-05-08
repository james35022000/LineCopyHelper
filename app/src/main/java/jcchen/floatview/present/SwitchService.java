package jcchen.floatview.present;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by JCChen on 2018/5/6.
 */

public class SwitchService extends Service {

    public static final int MODE_EXIT = -1;
    public static final int MODE_ON = 0;
    public static final int MODE_OFF = 1;
    private int mode;

    @Override
    public void onCreate() {
        super.onCreate();
        mode = MODE_OFF;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new SwitchBinder();
    }

    public class SwitchBinder extends Binder {
        public SwitchService getService() {
            return SwitchService.this;
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
