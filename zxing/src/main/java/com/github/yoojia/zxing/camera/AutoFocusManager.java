package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 * @since 1.0
 */
public class AutoFocusManager implements Camera.AutoFocusCallback{

    private final Camera mCamera;
    private final AutoFocusListener mAutoFocusListener;
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

    public AutoFocusManager(Camera camera, AutoFocusListener autoFocusListener) {
        mCamera = camera;
        mAutoFocusListener = autoFocusListener;
        final String mode = camera.getParameters().getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode) || Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)){
            mAutoFocusEnabled = true;
        }else{
            mAutoFocusEnabled = false;
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        mAutoFocusListener.onFocus(success);
    }

    public void requestAutoFocus(){
        mCamera.autoFocus(this);
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
