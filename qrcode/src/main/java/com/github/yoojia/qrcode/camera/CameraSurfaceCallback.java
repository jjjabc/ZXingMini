package com.github.yoojia.qrcode.camera;

import android.view.SurfaceHolder;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @date :   2015-03-03
 * 照相机预览接口，隐藏无用的接口
 */
public abstract class CameraSurfaceCallback implements SurfaceHolder.Callback {

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
