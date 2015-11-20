package com.github.yoojia.zxing.qrcode;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.github.yoojia.zxing.camera.CameraController;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @since 1.0
 * 封装扫描支持功能
 */
public class QRCodeSupport {

    public static final String TAG = QRCodeSupport.class.getSimpleName();

    private final CameraController mCameraController;
    private final Decoder mQRCodeDecode = new Decoder.Builder().build();
    private ImageView mCapturePreview = null;
    private OnResultListener mOnResultListener;

    public QRCodeSupport(SurfaceView surfaceView) {
        this(surfaceView, null);
    }

    public QRCodeSupport(SurfaceView surfaceView, OnResultListener listener) {
        mCameraController = new CameraController(surfaceView);
        mCameraController.setPreviewCallback(new Camera.PreviewCallback() {
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
        mOnResultListener = listener;
    }

    public void onResume(){
        mCameraController.resume();
    }

    public void onPause(){
        mCameraController.pause();
    }

    public void setOnResultListener(OnResultListener onResultListener) {
        mOnResultListener = onResultListener;
    }

    public void setCapturePreview(ImageView capturePreview){
        mCapturePreview = capturePreview;
    }

    public void requestDecode(){
        mCameraController.getCameraManager().getFocusManager().requestAutoFocus();
    }

    public void startAuto(int period){
        mCameraController.getCameraManager().getFocusManager().startAutoFocus(period);
    }

    public void stopAuto(){
        mCameraController.getCameraManager().getFocusManager().stopAutoFocus();
    }

    private class PreviewQRCodeDecodeTask extends DecodeTask {

        public PreviewQRCodeDecodeTask(Decoder qrCodeDecode) {
            super(qrCodeDecode);
        }

        @Override
        protected void onPostDecoded(String result) {
            if (mOnResultListener == null){
                Log.w(TAG, "WARNING ! QRCode result ignored !");
            }else{
                mOnResultListener.onScanResult(result);
            }
        }

        @Override
        protected void onDecodeProgress(Bitmap capture) {
            if (mCapturePreview != null){
                mCapturePreview.setImageBitmap(capture);
            }
        }
    }

    public interface OnResultListener {
        void onScanResult(String notNullResult);
    }
}
