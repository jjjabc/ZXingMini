package com.github.yoojia.zxing.camera;

import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 * @since 1.0
 */
public class Cameras {

    private final static String TAG = Cameras.class.getSimpleName();

    private final SurfaceView mPreviewSurfaceView;
    private final CameraManager mCameraManager;
    private final Handler mHandler = new Handler();
    private final Runnable mTask = new Runnable(){
        @Override public void run() {
            Log.d(TAG, "- NOW open camera and start preview...");
            try {
                mCameraManager.open();
            } catch (Exception e) {
                Log.e(TAG, "- Cannot open camera", e);
            }
            mCameraManager.startPreview();
            try {
                mCameraManager.attachPreview(mPreviewSurfaceView.getHolder());
            } catch (IOException e) {
                Log.e(TAG, "- Cannot attach to preview", e);
            }
        }
    };
    private final SurfaceViewReadyCallback mViewReadyCallback = new SurfaceViewReadyCallback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mCameraManager.isOpen()) {
                return;
            }
            Log.d(TAG, "- Preview SurfaceView NOW ready, open camera by CameraManager");
            mTask.run();
        }
    };

    private int mStartCameraDelay = 10;

    public Cameras(SurfaceView previewSurfaceView) {
        mPreviewSurfaceView = previewSurfaceView;
        mCameraManager = new CameraManager(previewSurfaceView.getContext());
    }

    public void start(){
        Log.d(TAG, "- Try open camera and start preview...");
        mHandler.postDelayed(mTask, mStartCameraDelay);
    }

    public void stop(){
        Log.d(TAG, "- Try stop preview and close camera...");
        mHandler.removeCallbacks(mTask);
        final SurfaceHolder holder = mPreviewSurfaceView.getHolder();
        holder.removeCallback(mViewReadyCallback);
        mCameraManager.stopPreview();
        try {
            mCameraManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设定在SurfaceView回调无效的情况下,重启相机的延时时间
     * @param delayMs 延时时间
     */
    public void setStartCameraDelay(int delayMs) {
        mStartCameraDelay = delayMs;
    }

}
