package jcchen.LineCopyHelper.view;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import jcchen.LineCopyHelper.present.SwitchService;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Settings.canDrawOverlays(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }
        else if(!isAccessibilitySettingsOn()) {
            openAccessibility();
        }
        else {
            Log.d(TAG, "Start FloatView");
            startService(new Intent(getBaseContext(), FloatViewService.class));
            Log.d(TAG, "Start SwitchService");
            startService(new Intent(getBaseContext(), SwitchService.class));
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    private boolean isAccessibilitySettingsOn() {
        try {

            if (Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1) {
                TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
                String settingValue = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    mStringColonSplitter.setString(settingValue);
                    while (mStringColonSplitter.hasNext()) {
                        String accessibilityService = mStringColonSplitter.next();
                        if (accessibilityService.equalsIgnoreCase("jcchen.LineCopyHelper/jcchen.LineCopyHelper.model.LineCopyService")) {
                            Log.d(TAG, "We've found the correct setting - accessibility is switched on!");
                            return true;
                        }
                    }
                }
            }
            else {
                Log.d(TAG,"Accessibility service disable");
            }
        } catch(Exception e) {
            Log.d(TAG, "get accessibility enable failed, the err:" + e.getMessage());
        }
        return false;
    }

    private void openAccessibility(){
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        this.startActivity(intent);
    }
}
