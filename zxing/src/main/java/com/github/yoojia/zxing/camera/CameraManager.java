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

import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private final Context mContext;

    private Camera mCamera;
    private AutoFocusManager mAutoFocusManager;

    private Rect mFramingRect;
    private Rect mFramingRectInPreview;

    private boolean mInitialized;
    private boolean mPreviewing;

    private int mRequestedFramingRectWidth;
    private int mRequestedFramingRectHeight;

    private Point mScreenResolution;
    private Point mCameraResolution;

    public CameraManager(Context context) {
        this.mContext = context;
    }

    public AutoFocusManager getAutoFocusManager() {
        return mAutoFocusManager;
    }

    public void requestPreview(Camera.PreviewCallback callback){
        mCamera.setOneShotPreviewCallback(callback);
    }

    /**
     * Opens the mCamera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the mCamera will draw preview frames into.
     * @throws java.io.IOException Indicates the mCamera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        Camera camera = mCamera;
        if (camera == null) {
            camera = OpenCameraInterface.open(OpenCameraInterface.NO_REQUESTED_CAMERA);
            if (camera == null) {
                throw new IOException("Fail to open camera device !");
            }
            mCamera = camera;
        }
        camera.setPreviewDisplay(holder);
        // 设置预览方向。注意：此设置不会影响到PreviewCallback回调、及其生成的Bitmap图片的数据方向，
        camera.setDisplayOrientation(90);
        if (!mInitialized) {
            mInitialized = true;
            initFromCameraParameters(camera);
            if (mRequestedFramingRectWidth > 0 && mRequestedFramingRectHeight > 0) {
                setManualFramingRect(mRequestedFramingRectWidth, mRequestedFramingRectHeight);
                mRequestedFramingRectWidth = 0;
                mRequestedFramingRectHeight = 0;
            }
        }
        Camera.Parameters parameters = camera.getParameters();
        String parametersFlattened = parameters.flatten();
        try {
            setDesiredCameraParameters(camera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.e(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.e(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            parameters = camera.getParameters();
            parameters.unflatten(parametersFlattened);
            try {
                camera.setParameters(parameters);
                setDesiredCameraParameters(camera, true);
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
    public synchronized void closeDriver() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            // Make sure to clear these each time we close the mCamera, so that any scanning rect
            // requested by intent is forgotten.
            mFramingRect = null;
            mFramingRectInPreview = null;
        }
    }

    /**
     * Asks the mCamera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview(AutoFocusListener autoFocusListener) {
        Camera camera = mCamera;
        if (camera != null && !mPreviewing) {
            camera.startPreview();
            mPreviewing = true;
            mAutoFocusManager = new AutoFocusManager(mCamera, autoFocusListener);
        }
    }

    /**
     * Tells the mCamera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (mAutoFocusManager != null) {
            mAutoFocusManager.stopAutoFocus();
            mAutoFocusManager = null;
        }
        if (mCamera != null && mPreviewing) {
            mCamera.stopPreview();
            mPreviewing = false;
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (mFramingRect == null) {
            if (mCamera == null) {
                return null;
            }
            if (mScreenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            int width = findDesiredDimensionInRange(mScreenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(mScreenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

            int leftOffset = (mScreenResolution.x - width) / 2;
            int topOffset = (mScreenResolution.y - height) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated framing rect: " + mFramingRect);
        }
        return mFramingRect;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link android.graphics.Rect} expressing barcode scan area in terms of the preview size
     */
    public synchronized Rect getFramingRectInPreview() {
        if (mFramingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            if (mCameraResolution == null || mScreenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            rect.left = rect.left * mCameraResolution.x / mScreenResolution.x;
            rect.right = rect.right * mCameraResolution.x / mScreenResolution.x;
            rect.top = rect.top * mCameraResolution.y / mScreenResolution.y;
            rect.bottom = rect.bottom * mCameraResolution.y / mScreenResolution.y;
            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width  The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
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
            mFramingRectInPreview = null;
        } else {
            mRequestedFramingRectWidth = width;
            mRequestedFramingRectHeight = height;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
    }


    /**
     * Reads, one time, values from the mCamera that are needed by the app.
     */
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
