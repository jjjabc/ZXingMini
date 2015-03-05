package com.github.yoojia.zxing;

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

import java.io.IOException;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-05
 * 封装扫描支持功能
 */
public class QRCodeScanSupport {

    public static final String TAG = QRCodeScanSupport.class.getSimpleName();

    private final CameraManager mCameraManager;
    private final SurfaceView mSurfaceView;
    private final QRCodeDecode mQRCodeDecode = new QRCodeDecode.Builder().build();;
    private ImageView mCapturePreview = null;
    private OnScanResultListener mOnScanResultListener;

    private final CameraSurfaceCallback mCallback = new CameraSurfaceCallback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initCamera(holder);
        }
    };

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
            QRCodeDecodeTask.CameraPreview preview = new QRCodeDecodeTask.CameraPreview(data, camera);
            mDecodeTask.execute(preview);
        }
    };

    /**
     * 自动对焦结果回调
     */
    private final AutoFocusListener mAutoFocusListener = new AutoFocusListener() {
        @Override
        public void onFocus(boolean focusSuccess) {
            // 对焦成功后，请求触发生成 **一次** 预览图片
            if (focusSuccess) mCameraManager.requestPreview(mPreviewCallback);
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
        mCameraManager = new CameraManager(surfaceView.getContext().getApplicationContext());
        finderView.setCameraManager(mCameraManager);
        mSurfaceView = surfaceView;
        mOnScanResultListener = listener;
    }

    /**
     * 在Activity的onResume中调用
     * @param activity Activity
     */
    public void onResume(Activity activity){
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(mCallback);
    }

    /**
     * 在Activity的onPause中调用
     * @param activity Activity
     */
    public void onPause(Activity activity){
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.removeCallback(mCallback);
        // 关闭摄像头
        mCameraManager.stopPreview();
        mCameraManager.closeDriver();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (mCameraManager.isOpen()) return;
        try {
            mCameraManager.openDriver(surfaceHolder);
            mCameraManager.requestPreview(mPreviewCallback);
            mCameraManager.startPreview(mAutoFocusListener);
        }catch (IOException ioe) {
            Log.w(TAG, ioe);
        }
    }

    private class PreviewQRCodeDecodeTask extends QRCodeDecodeTask{

        public PreviewQRCodeDecodeTask(QRCodeDecode qrCodeDecode) {
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
