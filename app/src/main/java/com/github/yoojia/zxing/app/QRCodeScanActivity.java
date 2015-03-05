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

import com.github.yoojia.zxing.QRCodeScanSupport;
import com.github.yoojia.zxing.R;
import com.github.yoojia.zxing.FinderView;
import com.github.yoojia.zxing.QRCodeDecode;
import com.github.yoojia.zxing.QRCodeDecodeTask;
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

    private QRCodeScanSupport mQRCodeScanSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);

        ImageView capturePreview = (ImageView) findViewById(R.id.decode_preview);
        final FinderView finderView = (FinderView) findViewById(R.id.capture_viewfinder_view);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view);

        mQRCodeScanSupport = new QRCodeScanSupport(surfaceView, finderView);
        mQRCodeScanSupport.setCapturePreview(capturePreview);

        // 如何处理扫描结果
        mQRCodeScanSupport.setOnScanResultListener(new QRCodeScanSupport.OnScanResultListener() {
            @Override
            public void onScanResult(String notNullResult) {
                Toast.makeText(QRCodeScanActivity.this, "扫描结果: " + notNullResult, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onResume() {
        mQRCodeScanSupport.onResume(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mQRCodeScanSupport.onPause(this);
        super.onPause();
    }
}
