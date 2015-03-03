package com.github.yoojia.zxing.camera;

/**
 * @author :   Yoojia.Chen (yoojiachen@163.com)
 * @date :   2015-03-02
 * 自动聚焦回调接口
 */
public interface AutoFocusListener {

    /**
     * 聚集完成时此接口被回调
     * @param focusSuccess 是否聚集成功
     */
    void onFocus(boolean focusSuccess);
}
