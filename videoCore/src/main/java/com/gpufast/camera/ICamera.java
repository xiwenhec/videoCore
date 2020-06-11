package com.gpufast.camera;

/**
 * @author Sivin 2018/10/26
 * Description:
 */
 interface ICamera {

    int CAMERA_FRONT = 0;
    int CAMERA_BACK = 1;

    /**
     * 设置相机参数
     * @param params
     */
    void setCameraParams(CameraParams params);


    void openCamera(int orientation);

    /**
     * 前后摄像头切换
     */
    void switchCamera();

    /**
     * 开始预览数据
     */
    void startPreview();

    /**
     * 停止预览数据
     */
    void stopPreview();

    /**
     * 停止摄像头
     */
    void stopCamera();
}
