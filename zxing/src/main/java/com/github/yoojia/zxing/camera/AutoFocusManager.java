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

    private final AtomicInteger mScheduleMs = new AtomicInteger(0);

    private final Handler mAutoFocusHandler = new Handler(Looper.getMainLooper());

    private final Runnable mAutoFocusTask = new Runnable() {
        @Override public void run() {
            requestAutoFocus();
            final int delay = mScheduleMs.get();
            if (delay > 0) {
                startAutoFocus(delay);
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
        if (ms < 1000) {
            throw new IllegalArgumentException("Auto Focus period tim to short !");
        }
        mAutoFocusHandler.removeCallbacks(mAutoFocusTask);
        if (0 == mScheduleMs.get()){
            mAutoFocusHandler.postDelayed(mAutoFocusTask, ms);
        }else{
            mAutoFocusHandler.post(mAutoFocusTask);
        }
        mScheduleMs.set(ms);
    }

    public void stopAutoFocus(){
        mScheduleMs.set(0);
        mAutoFocusHandler.removeCallbacks(mAutoFocusTask);
    }

}
