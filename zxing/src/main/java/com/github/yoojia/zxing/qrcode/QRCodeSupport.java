package com.github.yoojia.zxing.qrcode;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.github.yoojia.zxing.camera.Cameras;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-05
 * 封装扫描支持功能
 */
public class QRCodeSupport {

    public static final String TAG = QRCodeSupport.class.getSimpleName();

    private final Cameras mCameras;
    private final Decoder mQRCodeDecode = new Decoder.Builder().build();
    private ImageView mCapturePreview = null;
    private OnScanResultListener mOnScanResultListener;

    public QRCodeSupport(SurfaceView surfaceView, FinderView finderView) {
        this(surfaceView, finderView, null);
    }

    public QRCodeSupport(SurfaceView surfaceView, FinderView finderView, OnScanResultListener listener) {
        mCameras = new Cameras(surfaceView);
        finderView.setCameraManager(mCameras.getCameraManager());
        mCameras.setPreviewCallback(new Camera.PreviewCallback() {
            private PreviewQRCodeDecodeTask mDecodeTask;
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mDecodeTask != null) {
                    mDecodeTask.cancel(true);
                }
                mDecodeTask = new PreviewQRCodeDecodeTask(mQRCodeDecode);
                DecodeTask.CameraPreview preview = new DecodeTask.CameraPreview(data, camera);
                mDecodeTask.execute(preview);
            }
        });
        mOnScanResultListener = listener;
    }

    public void onResume(){
        mCameras.onResume();
    }

    public void onPause(){
        mCameras.onPause();
    }

    public void setOnScanResultListener(OnScanResultListener onScanResultListener) {
        mOnScanResultListener = onScanResultListener;
    }

    public void setCapturePreview(ImageView capturePreview){
        mCapturePreview = capturePreview;
    }

    public void requestDecode(){
        mCameras.getCameraManager().getAutoFocusManager().requestAutoFocus();
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
