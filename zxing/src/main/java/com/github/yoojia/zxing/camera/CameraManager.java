/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.yoojia.zxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 * @author dswitkin@google.com (Daniel Switkin)
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private final Context mContext;

    private Camera mCamera;
    private FocusManager mFocusManager;

    private Rect mFramingRect;

    private boolean mInitialized;
    private boolean mPreviewing;

    private int mRequestedFramingRectWidth;
    private int mRequestedFramingRectHeight;

    private Point mScreenResolution;
    private Point mCameraResolution;

    public CameraManager(Context context) {
        this.mContext = context;
    }

    public FocusManager getFocusManager() {
        return mFocusManager;
    }

    public void requestPreview(Camera.PreviewCallback callback){
        mCamera.setOneShotPreviewCallback(callback);
    }

    public synchronized void open(SurfaceHolder holder) throws IOException {
        if (mCamera == null) {
            mCamera = OpenCameraInterface.open(OpenCameraInterface.NO_REQUESTED_CAMERA);
            if (mCamera == null) {
                throw new IOException("Fail to open camera device !");
            }
        }
        mCamera.setPreviewDisplay(holder);
        // 设置预览方向。注意：此设置不会影响到PreviewCallback回调、及其生成的Bitmap图片的数据方向，
        mCamera.setDisplayOrientation(90);
        if (!mInitialized) {
            mInitialized = true;
            initFromCameraParameters(mCamera);
            if (mRequestedFramingRectWidth > 0 && mRequestedFramingRectHeight > 0) {
                setManualFramingRect(mRequestedFramingRectWidth, mRequestedFramingRectHeight);
                mRequestedFramingRectWidth = 0;
                mRequestedFramingRectHeight = 0;
            }
        }
        Camera.Parameters parameters = mCamera.getParameters();
        String parametersFlattened = parameters.flatten();
        try {
            setDesiredCameraParameters(mCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.e(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.e(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            parameters = mCamera.getParameters();
            parameters.unflatten(parametersFlattened);
            try {
                mCamera.setParameters(parameters);
                setDesiredCameraParameters(mCamera, true);
            } catch (RuntimeException re2) {
                // Well, darn. Give up
                Log.e(TAG, "> Camera rejected even safe-mode parameters! No configuration");
            }
        }
    }

    /**
     * @return 返回相机是否已开启
     */
    public synchronized boolean isOpen() {
        return mCamera != null;
    }

    /**
     * 如果相机被使用，则关闭它
     */
    public synchronized void close() {
        if (isOpen()) {
            mCamera.release();
            mCamera = null;
            mFramingRect = null;
        }
    }

    public synchronized void startPreview(FocusEventsListener focusEventsListener) {
        if (mCamera != null && !mPreviewing) {
            mCamera.startPreview();
            mPreviewing = true;
            mFocusManager = new FocusManager(mCamera, focusEventsListener);
        }
    }

    public synchronized void stopPreview() {
        if (mFocusManager != null) {
            mFocusManager.stopAutoFocus();
            mFocusManager = null;
        }
        if (mCamera != null && mPreviewing) {
            mCamera.stopPreview();
            mPreviewing = false;
        }
    }

    public synchronized void setManualFramingRect(int width, int height) {
        if (mInitialized) {
            if (width > mScreenResolution.x) {
                width = mScreenResolution.x;
            }
            if (height > mScreenResolution.y) {
                height = mScreenResolution.y;
            }
            int leftOffset = (mScreenResolution.x - width) / 2;
            int topOffset = (mScreenResolution.y - height) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + mFramingRect);
        } else {
            mRequestedFramingRectWidth = width;
            mRequestedFramingRectHeight = height;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    private void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        mScreenResolution = theScreenResolution;
        Log.i(TAG, "Screen resolution: " + mScreenResolution);
        mCameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, mScreenResolution);
        Log.i(TAG, "Camera resolution: " + mCameraResolution);

    }

    private void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setFocus( parameters,
                true, // auto focus
                true, // disable continuous
                safeMode);
        parameters.setPreviewSize(mCameraResolution.x, mCameraResolution.y);
        camera.setParameters(parameters);
        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (mCameraResolution.x != afterSize.width || mCameraResolution.y != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + mCameraResolution.x + 'x' + mCameraResolution.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            mCameraResolution.x = afterSize.width;
            mCameraResolution.y = afterSize.height;
        }
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }


}
