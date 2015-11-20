package com.github.yoojia.zxing.camera;

import android.hardware.Camera;

@Deprecated
public class AutoFocusManager extends FocusManager{

    public AutoFocusManager(Camera camera, FocusEventsListener focusEventsListener) {
        super(camera, focusEventsListener);
    }
}
