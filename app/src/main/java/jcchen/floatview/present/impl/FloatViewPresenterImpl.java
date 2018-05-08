package jcchen.floatview.present.impl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.Toast;

import jcchen.floatview.R;
import jcchen.floatview.present.FloatViewListener;
import jcchen.floatview.present.FloatViewPresenter;
import jcchen.floatview.present.SwitchService;
import jcchen.floatview.view.FloatView;
import jcchen.floatview.view.RoundedImageView;

/**
 * Created by JCChen on 2018/5/5.
 */

public class FloatViewPresenterImpl implements FloatViewPresenter {
    private static final String TAG = FloatViewPresenterImpl.class.getName();
    private Context context;
    private float Density;

    private final boolean MODE_ON = true;
    private final boolean MODE_OFF = false;
    private boolean MODE = MODE_OFF;
    private boolean EXIT = MODE_OFF;

    private static final int LEFT_SIDE = -1;
    private static final int RIGHT_SIDE = 1;
    private int POS;

    private boolean isBind;
    private SwitchService switchService;
    private FloatView floatView;

    private ServiceConnection mConnection;

    Animation translateAnimation = null;
    ValueAnimator animator = null;

    public FloatViewPresenterImpl(Context context, FloatView floatView) {
        this.context = context;
        this.floatView = floatView;
        Density = context.getResources().getDisplayMetrics().density;

        isBind = false;
        if(!isMyServiceRunning(SwitchService.class)) {
            context.startService(new Intent(context, SwitchService.class));
        }

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                SwitchService.SwitchBinder binder = (SwitchService.SwitchBinder) service;
                switchService = binder.getService();
                isBind = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                switchService = null;
                isBind = false;
            }
        };

        Intent intent = new Intent(context, SwitchService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void serviceSwitch(FloatViewListener floatViewListener) {
        if(!isBind) {
            Log.d(TAG, "Lost connection...");
            if(!isMyServiceRunning(SwitchService.class)) {
                context.startService(new Intent(context, SwitchService.class));
                Log.d(TAG, "Restart service");
            }
            Intent intent = new Intent(context, SwitchService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            return;
        }
        if(MODE == MODE_OFF) {
            MODE = MODE_ON;
            switchService.setMode(SwitchService.MODE_ON);
            Log.d(TAG, "Switch MODE to ON");
            floatViewListener.onSwitchOn();
        }
        else {
            MODE = MODE_OFF;
            switchService.setMode(SwitchService.MODE_OFF);
            Log.d(TAG, "Switch MODE to OFF");
            floatViewListener.onSwitchOff();
        }
    }

    @Override
    public void exit() {
        Log.d(TAG, "Application exit.");
        if(animator != null) {
            animator.cancel();
            animator = null;
        }
        if(translateAnimation != null) {
            translateAnimation.cancel();
            translateAnimation = null;
        }
        context.unbindService(mConnection);
        switchService.setMode(SwitchService.MODE_EXIT);
        context.stopService(new Intent(context, SwitchService.class));
        ((Service)context).stopSelf();
    }

    @Override
    public void onDestroy() {
        context = null;
        floatView = null;
        switchService = null;
        mConnection = null;
    }

    @Override
    public void showExitOption() {
        EXIT = MODE_ON;
        Toast.makeText(context, "Are you sure want to exit?", Toast.LENGTH_SHORT).show();

        POS = floatView.getLayoutParams().x < floatView.widthPixels / 2 ? LEFT_SIDE : RIGHT_SIDE;

        final RelativeLayout exit_relativeLayout = (RelativeLayout) floatView.findViewById(R.id.exit_relativeLayout);
        final RelativeLayout icon_relativeLayout = (RelativeLayout) floatView.findViewById(R.id.icon_relativeLayout);
        final RoundedImageView background_RImageView = (RoundedImageView) floatView.findViewById(R.id.background_RImageView);
        final RoundedImageView icon_RImageView = (RoundedImageView) floatView.findViewById(R.id.icon_RImageView);

        floatView.setStatus(FloatView.STATUS_ANIM);

        if(POS == LEFT_SIDE) {
            floatView.getLayoutParams().width = icon_relativeLayout.getWidth() * 2 + floatView.interval;
            floatView.updateViewLayout();

            animator = ValueAnimator.ofFloat(0, icon_relativeLayout.getWidth() + floatView.interval);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    exit_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                    icon_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                    floatView.updateViewLayout();
                }
            });
        }
        else {
            animator = ValueAnimator.ofInt(floatView.getLayoutParams().x, floatView.getLayoutParams().x - (icon_relativeLayout.getWidth() + floatView.interval));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    floatView.getLayoutParams().x = (int) animation.getAnimatedValue();
                    floatView.updateViewLayout();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    floatView.getLayoutParams().width = icon_relativeLayout.getWidth() * 2 + floatView.interval;
                    floatView.updateViewLayout();
                }
            });
        }
        animator.setDuration(300);
        animator.start();

        animator = POS == LEFT_SIDE ? ValueAnimator.ofFloat(icon_relativeLayout.getWidth() + floatView.interval, 0) :
                                      ValueAnimator.ofFloat(0, icon_relativeLayout.getWidth() + floatView.interval);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                exit_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                floatView.updateViewLayout();
            }
        });
        animator.setStartDelay(300);
        animator.setDuration(300);
        animator.start();

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                exit_relativeLayout.setScaleX((float) animation.getAnimatedValue());
                exit_relativeLayout.setScaleY((float) animation.getAnimatedValue());
                floatView.updateViewLayout();
            }
        });
        animator.setStartDelay(300);
        animator.setDuration(300);
        animator.start();

        final float[] from = new float[3];
        final float[] to = new float[3];
        final float[] hsv = new float[3];

        Color.colorToHSV(Color.parseColor("#" + Integer.toHexString(MODE == MODE_OFF ?
                ContextCompat.getColor(context, R.color.colorFloatViewStart) :
                ContextCompat.getColor(context, R.color.colorFloatViewStop))), from);
        Color.colorToHSV(Color.parseColor("#FF000000"), to);

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(context == null)  return;
                hsv[0] = from[0] + (to[0] - from[0]) * animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1]) * animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2]) * animation.getAnimatedFraction();
                background_RImageView.setColorFilter(Color.HSVToColor(hsv));

                if(animation.getAnimatedFraction() < 0.5f) {
                    icon_RImageView.setAlpha(1.0f - animation.getAnimatedFraction() * 2);
                }
                else {
                    icon_RImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_exit_yes));
                    icon_RImageView.setAlpha((animation.getAnimatedFraction() - 0.5f) * 2);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                floatView.setStatus(FloatView.STATUS_EXPAND);
            }
        });
        animator.setStartDelay(200);
        animator.setDuration(500);
        animator.start();

        exit_relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideExitOption();
            }
        });
    }

    public void hideExitOption() {
        EXIT = MODE_OFF;

        POS = floatView.getLayoutParams().x < floatView.widthPixels / 2 ? LEFT_SIDE : RIGHT_SIDE;

        final RelativeLayout exit_relativeLayout = (RelativeLayout) floatView.findViewById(R.id.exit_relativeLayout);
        final RelativeLayout icon_relativeLayout = (RelativeLayout) floatView.findViewById(R.id.icon_relativeLayout);
        final RoundedImageView background_RImageView = (RoundedImageView) floatView.findViewById(R.id.background_RImageView);
        final RoundedImageView icon_RImageView = (RoundedImageView) floatView.findViewById(R.id.icon_RImageView);

        floatView.setStatus(FloatView.STATUS_ANIM);

        animator = POS == RIGHT_SIDE ? ValueAnimator.ofFloat(icon_relativeLayout.getWidth() + floatView.interval, 0) :
                                      ValueAnimator.ofFloat(0, icon_relativeLayout.getWidth() + floatView.interval);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                exit_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                floatView.updateViewLayout();
            }
        });
        animator.setDuration(300);
        animator.start();

        animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                exit_relativeLayout.setScaleX(animation.getAnimatedFraction());
                exit_relativeLayout.setScaleY(animation.getAnimatedFraction());
                floatView.updateViewLayout();
            }
        });
        animator.setDuration(300);
        animator.start();

        final float[] from = new float[3];
        final float[] to = new float[3];
        final float[] hsv = new float[3];

        Color.colorToHSV(Color.parseColor("#FF000000"), from);
        Color.colorToHSV(Color.parseColor("#" + Integer.toHexString(MODE == MODE_OFF ?
                ContextCompat.getColor(context, R.color.colorFloatViewStart) :
                ContextCompat.getColor(context, R.color.colorFloatViewStop))), to);

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (context == null) return;
                hsv[0] = from[0] + (to[0] - from[0]) * animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1]) * animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2]) * animation.getAnimatedFraction();
                background_RImageView.setColorFilter(Color.HSVToColor(hsv));

                if (animation.getAnimatedFraction() < 0.5f) {
                    icon_RImageView.setAlpha(1.0f - animation.getAnimatedFraction() * 2);
                } else {
                    icon_RImageView.setImageDrawable(ContextCompat.getDrawable(context,
                            MODE == MODE_OFF ? R.drawable.ic_start : R.drawable.ic_stop));
                    icon_RImageView.setAlpha((animation.getAnimatedFraction() - 0.5f) * 2);
                }
            }
        });
        animator.setDuration(500);
        animator.start();

        if(POS == LEFT_SIDE) {
            animator = ValueAnimator.ofFloat(icon_relativeLayout.getWidth() + floatView.interval, 0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    exit_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                    icon_relativeLayout.setTranslationX((float) animation.getAnimatedValue());
                    floatView.updateViewLayout();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    floatView.getLayoutParams().width = icon_relativeLayout.getWidth();
                    floatView.updateViewLayout();
                    floatView.autoDrag(FloatView.AUTO_DRAG_MODE_SLOW);
                    floatView.setStatus(FloatView.STATUS_NORMAL);
                }
            });
        }
        else {
            animator = ValueAnimator.ofInt(floatView.getLayoutParams().x, floatView.getLayoutParams().x + (icon_relativeLayout.getWidth() + floatView.interval));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    floatView.getLayoutParams().x = (int) animation.getAnimatedValue();
                    floatView.updateViewLayout();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationEnd(animation);
                    floatView.getLayoutParams().width = icon_relativeLayout.getWidth();
                    floatView.updateViewLayout();
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    floatView.autoDrag(FloatView.AUTO_DRAG_MODE_SLOW);
                    floatView.setStatus(FloatView.STATUS_NORMAL);
                }
            });
        }
        animator.setDuration(300);
        animator.setStartDelay(300);
        animator.start();

        exit_relativeLayout.setOnClickListener(null);
    }

    public boolean getExitState() {
        return EXIT;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
