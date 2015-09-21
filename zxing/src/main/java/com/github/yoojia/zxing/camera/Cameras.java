package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 * @since 1.0
 */
public class Cameras {

    private final static String TAG = "CAMERAS";

    private final CameraManager mCameraManager;
    private final SurfaceView mSurfaceView;

    private final CameraSurfaceCallback mCameraSurfaceCallback = new CameraSurfaceCallback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initCamera(holder);
        }
    };

    private final AutoFocusListener mAutoFocusListener = new AutoFocusListener() {
        @Override
        public void onFocus(boolean focusSuccess) {
            // 对焦成功后，请求触发生成 **一次** 预览图片
            if (focusSuccess) {
                mCameraManager.requestPreview(mPreviewCallback);
            }
        }
    };

    private Camera.PreviewCallback mPreviewCallback;

    public Cameras(SurfaceView surfaceView) {
        mCameraManager = new CameraManager(surfaceView.getContext().getApplicationContext());
        mSurfaceView = surfaceView;
    }

    public void onResume(){
        final SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(mCameraSurfaceCallback);
    }

    public void onPause(){
        final SurfaceHolder holder = mSurfaceView.getHolder();
        holder.removeCallback(mCameraSurfaceCallback);
        mCameraManager.stopPreview();
        mCameraManager.closeDriver();
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    private void initCamera(SurfaceHolder holder){
        if (mCameraManager.isOpen()){
            return;
        }
        try {
            mCameraManager.openDriver(holder);
        }catch (IOException ioe) {
            Log.w(TAG, ioe);
        }
        if (mPreviewCallback != null){
            mCameraManager.requestPreview(mPreviewCallback);
        }
        mCameraManager.startPreview(mAutoFocusListener);
    }
}
