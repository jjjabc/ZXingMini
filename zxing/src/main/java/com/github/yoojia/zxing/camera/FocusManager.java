package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
public class FocusManager {

    private final FocusEventsListener mFocusEventsListener;

    private final AtomicInteger mPeriod = new AtomicInteger(0);

    private final Handler mFocusHandler = new Handler(Looper.getMainLooper());

    private AutoFocusTask mAutoFocusTask;

    private final Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mFocusEventsListener.onFocus(success);
        }
    };

    /**
     * 创建对焦管理器,并指定对焦事件监听接口
     * @param focusEventsListener 对焦事件监听接口
     */
    public FocusManager(FocusEventsListener focusEventsListener) {
        mFocusEventsListener = focusEventsListener;
    }

    /**
     * 请求相机执行对焦动作
     * @param camera 相机对象
     */
    public void requestAutoFocus(Camera camera){
        camera.autoFocus(mAutoFocusCallback);
    }

    /**
     * 开启定时自动对焦
     * @param camera 相机对象
     * @param ms 定时,单位:毫秒
     */
    public void startAutoFocus(Camera camera, int ms){
        if (ms < 100) {
            throw new IllegalArgumentException("Auto Focus period time must more than 100ms !");
        }
        final String mode = camera.getParameters().getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode) || Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)){
            // Remove pre task
            if (mAutoFocusTask != null) {
                mFocusHandler.removeCallbacks(mAutoFocusTask);
            }
            mAutoFocusTask = new AutoFocusTask(camera);
            mPeriod.set(ms);
            mFocusHandler.post(mAutoFocusTask);
        }
    }

    /**
     * 停止自动对焦
     */
    public void stopAutoFocus(){
        mPeriod.set(0);
        mFocusHandler.removeCallbacks(mAutoFocusTask);
    }

    private class AutoFocusTask implements Runnable{

        private final Camera mCamera;

        private AutoFocusTask(Camera camera) {
            mCamera = camera;
        }

        @Override public void run() {
            requestAutoFocus(mCamera);
            final int period = mPeriod.get();
            if (period > 0) {
                mFocusHandler.postDelayed(mAutoFocusTask, period);
            }
        }
    }
}
