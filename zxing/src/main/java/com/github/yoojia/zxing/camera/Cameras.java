package com.github.yoojia.zxing.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
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

    private final FocusEventsListener mFocusEventsListener = new FocusEventsListener() {
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

    public Bitmap capture(CameraPreview cameraPreview){
        final Camera.Parameters parameters = cameraPreview.camera.getParameters();
        final int width = parameters.getPreviewSize().width;
        final int height = parameters.getPreviewSize().height;
        final YuvImage yuv = new YuvImage(cameraPreview.data, parameters.getPreviewFormat(), width, height, null);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);// Best
        final byte[] bytes = out.toByteArray();
        final Bitmap src = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        final Matrix matrix = new Matrix();
        matrix.setRotate(90);
        final int originWidth = src.getWidth();
        final int originHeight = src.getHeight();
        final int targetWH = originWidth > originHeight ? originHeight : originWidth;
        final int offsetX = originWidth > originHeight ? (originWidth - originHeight): 0;
        final int offsetY = originWidth > originHeight ? 0 : (originHeight - originWidth);
        return Bitmap.createBitmap(src, offsetX, offsetY, targetWH, targetWH, matrix, true);
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
        mCameraManager.startPreview(mFocusEventsListener);
    }

    public static final class CameraPreview{

        private final byte[] data;
        private final Camera camera;

        public CameraPreview(byte[] data, Camera camera) {
            this.data = data;
            this.camera = camera;
        }
    }
}
