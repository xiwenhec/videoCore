package com.gpufast.camera;

import android.hardware.Camera;

import com.gpufast.logger.ELog;

import java.io.IOException;
import java.util.List;

/**
 * @author Sivin 2018/10/26
 * Description:使用旧版本camera类API实现实现,
 * TODO：该类没有实现数据回调的接口，目前有待测试，能否实现数据回调是否与帧数据同时返回
 */
class Camera19 implements ICamera {
    private Camera mCamera = null;
    private int mCameraFace;
    private boolean isPreviewing = false;
    private CameraParams mParams;

    @Override
    public void setCameraParams(CameraParams params) {
        mParams = params;
    }

    private boolean openFrontCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                mCameraFace = Camera.CameraInfo.CAMERA_FACING_FRONT;
                return true;
            }
        }
        return false;
    }

    private boolean openBackCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i);
                mCameraFace = Camera.CameraInfo.CAMERA_FACING_BACK;
                return true;
            }
        }
        return false;
    }


    @Override
    public void switchCamera() {
        if (mCamera != null) {
            stopCamera();
        }
        if (mCameraFace == Camera.CameraInfo.CAMERA_FACING_BACK) {
            openFrontCamera();//打开当前选中的摄像头
        } else {
            openBackCamera();//打开当前选中的摄像头
        }
        setCameraParameter();
        startPreview();
    }

    public void openCamera(int orientation) {
        if (mCamera != null) {
            stopCamera();
        }
        if (orientation == ICamera.CAMERA_FRONT) {
            openFrontCamera();//打开当前选中的摄像头
        } else {
            openBackCamera();//打开当前选中的摄像头
        }
        setCameraParameter();
    }


    @Override
    public void startPreview() {
        if (mCamera == null) return;
        if (isPreviewing) {
            stopPreview();
        }
        if (mParams == null || mParams.getPreTexture() == null) return;
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewTexture(mParams.getPreTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        isPreviewing = false;
    }


    private void setCameraParameter() {
        if (mCamera == null) return;
        Camera.Parameters parameters = mCamera.getParameters();


        //获取支持的预览尺寸
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size preViewSize = CameraUtils.chooseOptimalSize(supportedPreviewSizes,
                mParams.getWidth(), mParams.getHeight());
        parameters.setPreviewSize(preViewSize.width, preViewSize.height);

        // 设置摄像头为自动聚焦
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();

        int[] fps = CameraUtils.chooseOptimalFps(supportedPreviewFpsRange, 25, 30);
        if(fps != null){
            ELog.d(Camera19.class,"fpsRange->min:"+fps[0]+" max:"+fps[1]);
            parameters.setPreviewFpsRange(fps[0],fps[1]);
        }
        mCamera.setParameters(parameters);
    }

    @Override
    public void stopCamera() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
    }
}
