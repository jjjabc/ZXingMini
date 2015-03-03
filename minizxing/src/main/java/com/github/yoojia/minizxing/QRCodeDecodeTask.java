package com.github.yoojia.minizxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-03
 * 二维码解码线程
 */
public abstract class QRCodeDecodeTask extends AsyncTask<QRCodeDecodeTask.CameraPreview, Bitmap, String>{

    private final QRCodeDecode mQRCodeDecode;

    public QRCodeDecodeTask(QRCodeDecode qrCodeDecode) {
        mQRCodeDecode = qrCodeDecode;
    }

    @Override
    final protected String doInBackground(CameraPreview... params) {
        final Bitmap progress = capture(params[0]);
        this.publishProgress(progress);
        return mQRCodeDecode.decode(progress);
    }

    @Override
    final protected void onPostExecute(String s) {
        if (!TextUtils.isEmpty(s)){
            onPostDecoded(s);
        }
    }

    /**
     * 解码完成
     * @param result 解码完成
     */
    protected abstract void onPostDecoded(String result);

    @Override
    final protected void onProgressUpdate(Bitmap... values) {
        onDecodeProgress(values[0]);
    }

    /**
     * 解码的图片
     * @param capture 图片
     */
    protected void onDecodeProgress(Bitmap capture){

    }

    private Bitmap capture(CameraPreview cameraPreview){
        final Camera.Parameters parameters = cameraPreview.mCamera.getParameters();
        final int width = parameters.getPreviewSize().width;
        final int height = parameters.getPreviewSize().height;
        final YuvImage yuv = new YuvImage(cameraPreview.mData, parameters.getPreviewFormat(), width, height, null);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);// Best
        final byte[] bytes = out.toByteArray();
        final Bitmap src = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        final Matrix matrix = new Matrix();
        matrix.setRotate(90);
        // 取图片中间部分
        final int originWidth = src.getWidth();
        final int originHeight = src.getHeight();
        final int targetWH = originWidth > originHeight ? originHeight : originWidth;
        final int offsetX = originWidth > originHeight ? (originWidth - originHeight) / 2 : 0;
        final int offsetY = originWidth > originHeight ? 0 : (originHeight - originWidth) / 2;
        // 将相机预览图片通过 progress 过程传递给上层
        return Bitmap.createBitmap(src, offsetX, offsetY, targetWH, targetWH, matrix, true);
    }

    public static final class CameraPreview{

        private final byte[] mData;
        private final Camera mCamera;

        public CameraPreview(byte[] data, Camera camera) {
            mData = data;
            mCamera = camera;
        }
    }
}
