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
    private boolean mIsSurfaceViewReady = false;
    private final Handler mHandler = new Handler();

    private final SurfaceViewReadyCallback mViewReadyCallback = new SurfaceViewReadyCallback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mCameraManager.isOpen()) {
                return;
            }
            mIsSurfaceViewReady = true;
            Log.d(TAG, "- Preview SurfaceView NOW ready, open camera by CameraManager");
            openCameraAndPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            super.surfaceDestroyed(holder);
            mIsSurfaceViewReady = false;
        }
    };

    public Cameras(SurfaceView previewSurfaceView) {
        mPreviewSurfaceView = previewSurfaceView;
        mCameraManager = new CameraManager(previewSurfaceView.getContext());
    }

    private void openCameraAndPreview() {
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

    public void start() {
        Log.d(TAG, "- Try open camera and start preview...");
        if (mIsSurfaceViewReady) {
            openCameraAndPreview();
        } else {
            final SurfaceHolder holder = mPreviewSurfaceView.getHolder();
            holder.addCallback(mViewReadyCallback);
        }
    }

    public void stop() {
        Log.d(TAG, "- Try stop preview and close camera...");
        final SurfaceHolder holder = mPreviewSurfaceView.getHolder();
        holder.removeCallback(mViewReadyCallback);
        mCameraManager.stopPreview();
        try {
            mCameraManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
