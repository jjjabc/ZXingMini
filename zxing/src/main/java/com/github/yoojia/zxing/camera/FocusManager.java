package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
public class FocusManager {

    private final Camera mCamera;
    private final FocusEventsListener mFocusEventsListener;
    private final boolean mAutoFocusEnabled;

    private final AtomicInteger mPeriod = new AtomicInteger(0);

    private final Handler mFocusHandler = new Handler(Looper.getMainLooper());

    private final Runnable mFocusTask = new Runnable() {
        @Override
        public void run() {
            requestAutoFocus();
            final int period = mPeriod.get();
            if (period > 0) {
                repeatAutoFocus(period);
            }
        }
    };

    private final Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mFocusEventsListener.onFocus(success);
        }
    };

    public FocusManager(Camera camera, FocusEventsListener focusEventsListener) {
        mCamera = camera;
        mFocusEventsListener = focusEventsListener;
        final String mode = camera.getParameters().getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode) || Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)){
            mAutoFocusEnabled = true;
        }else{
            mAutoFocusEnabled = false;
        }
    }

    public void requestAutoFocus(){
        mCamera.autoFocus(mAutoFocusCallback);
    }

    public void startAutoFocus(int ms){
        if (ms < 100) {
            throw new IllegalArgumentException("Auto Focus period time must more than 100ms !");
        }
        if( ! mAutoFocusEnabled) {
            return;
        }
        mFocusHandler.removeCallbacks(mFocusTask);
        mPeriod.set(ms);
        mFocusHandler.post(mFocusTask);
    }

    private void repeatAutoFocus(int period){
        mFocusHandler.postDelayed(mFocusTask, period);
    }

    public void stopAutoFocus(){
        mPeriod.set(0);
        mFocusHandler.removeCallbacks(mFocusTask);
    }

}
