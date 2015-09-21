package com.github.yoojia.zxing.qrcode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.github.yoojia.zxing.camera.AutoFocusListener;
import com.github.yoojia.zxing.camera.CameraManager;
import com.github.yoojia.zxing.camera.CameraSurfaceCallback;
import com.github.yoojia.zxing.camera.Cameras;

import java.io.IOException;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-05
 * 封装扫描支持功能
 */
public class QRCodeScanSupport {

    public static final String TAG = QRCodeScanSupport.class.getSimpleName();

//    private final CameraManager mCameraManager;
    private final Cameras mCameras;
    private final SurfaceView mSurfaceView;
    private final Decoder mQRCodeDecode = new Decoder.Builder().build();
    private ImageView mCapturePreview = null;
    private OnScanResultListener mOnScanResultListener;

    /**
     * 处理预览图片
     */
    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        private PreviewQRCodeDecodeTask mDecodeTask;

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mDecodeTask != null){
                mDecodeTask.cancel(true);
            }
            mDecodeTask = new PreviewQRCodeDecodeTask(mQRCodeDecode);
            DecodeTask.CameraPreview preview = new DecodeTask.CameraPreview(data, camera);
            mDecodeTask.execute(preview);
        }
    };

    /**
     * 设置扫描结果监听器
     * @param onScanResultListener 扫描结果监听器
     */
    public void setOnScanResultListener(OnScanResultListener onScanResultListener) {
        mOnScanResultListener = onScanResultListener;
    }

    /**
     * 设置显示预览截图的ImageView
     * @param capturePreview ImageView
     */
    public void setCapturePreview(ImageView capturePreview){
        this.mCapturePreview = capturePreview;
    }

    public QRCodeScanSupport(SurfaceView surfaceView, FinderView finderView) {
        this(surfaceView, finderView, null);
    }

    public QRCodeScanSupport(SurfaceView surfaceView, FinderView finderView, OnScanResultListener listener) {
        mCameras = new Cameras(surfaceView);
        mCameras.setPreviewCallback(mPreviewCallback);
        finderView.setCameraManager(mCameras.getCameraManager());
        mSurfaceView = surfaceView;
        mOnScanResultListener = listener;
    }

    public void onResume(){
        mCameras.onResume();
    }

    public void onPause(){
        mCameras.onPause();
    }

    private class PreviewQRCodeDecodeTask extends DecodeTask {

        public PreviewQRCodeDecodeTask(Decoder qrCodeDecode) {
            super(qrCodeDecode);
        }

        @Override
        protected void onPostDecoded(String result) {
            if (mOnScanResultListener == null){
                Log.w(TAG, "WARNING ! QRCode result ignored !");
            }else{
                mOnScanResultListener.onScanResult(result);
            }
        }

        @Override
        protected void onDecodeProgress(Bitmap capture) {
            if (mCapturePreview != null){
                mCapturePreview.setImageBitmap(capture);
            }
        }
    }

    public interface OnScanResultListener{
        void onScanResult(String notNullResult);
    }
}
