package com.github.yoojia.zxing.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.yoojia.zxing.R;
import com.github.yoojia.zxing.FinderView;
import com.github.yoojia.zxing.QRCodeDecode;
import com.github.yoojia.zxing.QRCodeDecodeTask;
import com.github.yoojia.zxing.ResultPointCallback;
import com.github.yoojia.zxing.camera.AutoFocusListener;
import com.github.yoojia.zxing.camera.CameraManager;
import com.github.yoojia.zxing.camera.CameraSurfaceCallback;

import java.io.IOException;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-03
 * 扫描二维码
 */
public class QRCodeScanActivity extends Activity{

    public static final String TAG = QRCodeScanActivity.class.getSimpleName();

    private CameraManager mCameraManager;
    private FinderView mViewfinderView;
    private SurfaceView mSurfaceView;
    private ImageView mCapturePreview;
    private QRCodeDecode mQRCodeDecode;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);

        mCapturePreview = (ImageView) findViewById(R.id.decode_preview);
        mViewfinderView = (FinderView) findViewById(R.id.capture_viewfinder_view);
        mSurfaceView = (SurfaceView) findViewById(R.id.capture_preview_view);

        mCameraManager = new CameraManager(getApplication());
        mViewfinderView.setCameraManager(mCameraManager);

        mQRCodeDecode = new QRCodeDecode.Builder()
                .setResultPointCallback(new ResultPointCallback(mViewfinderView))
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(mCallback);
    }

    @Override
    protected void onPause() {
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.removeCallback(mCallback);
        // 关闭摄像头
        mCameraManager.stopPreview();
        mCameraManager.closeDriver();
        super.onPause();
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
            Toast.makeText(QRCodeScanActivity.this, "扫描结果：\n" + result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onDecodeProgress(Bitmap capture) {
            mCapturePreview.setImageBitmap(capture);
        }
    }
}
