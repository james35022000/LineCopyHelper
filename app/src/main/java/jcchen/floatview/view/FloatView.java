package jcchen.floatview.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

import jcchen.floatview.R;

/**
 * Created by JCChen on 2018/5/5.
 */

public class FloatView extends RelativeLayout {
    private static final String TAG = FloatView.class.getName();

    private Context context;
    private float Density;
    public int interval;

    public static final int STATUS_EXPAND = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_TOUCH = 2;
    public static final int STATUS_ANIM = 3;
    private int Status = STATUS_NORMAL;
    private int origStatus = Status;

    public static final int AUTO_DRAG_MODE_THROW = 0;
    public static final int AUTO_DRAG_MODE_SLOW = 1;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    public int widthPixels, heightPixels;
    public int statusBarHeight;

    private float touchX, touchY;

    /* Detect action (Click, Long click, Throw) */
    private static final int THROW_PARAM = 40;
    private static final int LONG_CLICK_TIME = 500; /* ms */
    private float lastX, lastY;
    private float moveX, moveY;
    private float distanceX, distanceY;
    private long downTime, moveTime, upTime;
    private Timer timer;
    private boolean isLongClick;

    private View.OnClickListener onClickListener = null;
    private View.OnLongClickListener onLongClickListener = null;

    private ValueAnimator animatorX, animatorY;

    public FloatView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
        Density = context.getResources().getDisplayMetrics().density;
        interval = (int) (5f * Density);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();

        updateScreenInfo();
    }

    public FloatView(Context context) {
        super(context);
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        updateScreenInfo();
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        updateScreenInfo();

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(Status == STATUS_ANIM)  return true;
                origStatus = Status;
                Status = STATUS_TOUCH;
                isLongClick = false;
                if(animatorX != null)  animatorX.cancel();
                timer = new Timer(true);
                timer.schedule(new LongClickTimer(), LONG_CLICK_TIME);
                downTime = System.currentTimeMillis();
                touchX = event.getX();
                touchY = event.getY();

                /* Detect action */
                lastX = event.getRawX();
                lastY = event.getRawY();
                distanceX = 0;
                distanceY = 0;

                break;
            case MotionEvent.ACTION_MOVE:
                if(isLongClick)  return true;
                moveTime = System.currentTimeMillis();
                moveX = event.getRawX() - lastX;
                moveY = event.getRawY() - lastY;
                lastX = event.getRawX();
                lastY = event.getRawY();
                distanceX += Math.abs(moveX);
                distanceY += Math.abs(moveY);
                if(origStatus == STATUS_EXPAND)  return true;
                if(Status == STATUS_ANIM)  return true;
                updatePosition((int) (event.getRawX() - touchX), (int) (event.getRawY() - touchY - statusBarHeight));
                return true;
            case MotionEvent.ACTION_UP:
                if(Status == STATUS_ANIM)  return true;
                Status = origStatus;
                timer.cancel();

                if(isLongClick)  return true;

                // throw
                if((Math.abs(moveX) > THROW_PARAM || Math.abs(moveY) > THROW_PARAM) && Status != STATUS_EXPAND) {
                    Log.d(TAG, "Throw => " + moveX + " : " + moveY + " : " + (System.currentTimeMillis() - moveTime));
                    autoDrag(AUTO_DRAG_MODE_THROW);
                    return true;
                }
                upTime = System.currentTimeMillis();
                autoDrag(AUTO_DRAG_MODE_SLOW);

                if((upTime - downTime) < 0.1 * 1000L) {
                    Log.d(TAG, "Click => " + moveX + " : " + moveY + " : " + (System.currentTimeMillis() - moveTime));
                    onClick(event);
                }
                break;
        }
        return true;
    }

    private void updatePosition(int x, int y) {
        layoutParams.x = x > (widthPixels - getWidth()) ? (widthPixels - getWidth()) : (x < 0 ? 0 : x);
        layoutParams.y = y > (heightPixels - getHeight()) ? (heightPixels - getHeight()) : (y < 0 ? 0 : y);
        updateViewLayout();
    }

    public void updateViewLayout() {
        windowManager.updateViewLayout(this, layoutParams);
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return layoutParams;
    }

    public int getStatus() {
        return Status;
    }

    public void setStatus(int Status) {
        this.Status = Status;
        this.origStatus = Status;
    }

    private void updateScreenInfo() {
        Rect frame = new Rect();
        getWindowVisibleDisplayFrame(frame);
        statusBarHeight = frame.top;

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        widthPixels = metrics.widthPixels;
        heightPixels = metrics.heightPixels;
    }

    public void autoDrag(int mode) {
        if(Status == STATUS_EXPAND)  return;
        switch(mode) {
            case AUTO_DRAG_MODE_THROW:
                /*animatorX = ValueAnimator.ofInt(layoutParams.x, moveX < 0 ?
                        interval : widthPixels - interval - getWidth());
                animatorX.setDuration(500);
                animatorX.setInterpolator(new OvershootInterpolator(1f));
                animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        updatePosition((int) animation.getAnimatedValue(), layoutParams.y);
                    }
                });
                animatorX.start();

                animatorY = ValueAnimator.ofInt(layoutParams.y, layoutParams.y + (int) moveY * 2);
                animatorY.setDuration(500);
                animatorY.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        updatePosition(layoutParams.x, (int) animation.getAnimatedValue());
                    }
                });
                animatorY.start();
                break;*/
            case AUTO_DRAG_MODE_SLOW:
                animatorX = ValueAnimator.ofInt(layoutParams.x, layoutParams.x < widthPixels/2 ?
                        interval : widthPixels - interval - getWidth());
                animatorX.setDuration(500);
                animatorX.setInterpolator(new OvershootInterpolator(1f));
                animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        updatePosition((int) animation.getAnimatedValue(), layoutParams.y);
                    }
                });
                animatorX.start();

                if(layoutParams.y > heightPixels - interval - getHeight() || layoutParams.y < interval) {
                    animatorY = ValueAnimator.ofInt(layoutParams.y, layoutParams.y < interval ?
                            interval : heightPixels - interval - getHeight());
                    animatorY.setDuration(500);
                    animatorY.setInterpolator(new OvershootInterpolator(1f));
                    animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            updatePosition(layoutParams.x, (int) animation.getAnimatedValue());
                        }
                    });
                    animatorY.start();
                }
                break;
        }
    }

    private void onClick(MotionEvent event) {
        if(getStatus() != STATUS_EXPAND) {
            onClickListener.onClick(null);
        }
        else {
            boolean LEFT_SIDE = layoutParams.x < widthPixels / 2;
            if((LEFT_SIDE && event.getX() < getWidth() / 2) || (!LEFT_SIDE && event.getX() > getWidth() / 2))
                findViewById(R.id.exit_relativeLayout).performClick();
            else
                onClickListener.onClick(null);
        }
    }

    private class LongClickTimer extends TimerTask {
        public void run() {
            if(distanceX < interval * 2 && distanceY < interval * 2) {
                if(getStatus() == STATUS_TOUCH && origStatus == STATUS_NORMAL) {
                    isLongClick = true;
                    longClickHandler.sendMessage(new Message());
                }
            }
            cancel();
        }
    }

    private Handler longClickHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            autoDrag(AUTO_DRAG_MODE_SLOW);
            onLongClickListener.onLongClick(null);
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(30);
        }
    };

}
