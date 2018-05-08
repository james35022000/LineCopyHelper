package jcchen.LineCopyHelper.present;

/**
 * Created by JCChen on 2018/5/5.
 */

public interface FloatViewPresenter {
    void serviceSwitch(FloatViewListener floatViewListener);
    void exit();
    void showExitOption();
    void hideExitOption();
    boolean getExitState();

    void onDestroy();
}
