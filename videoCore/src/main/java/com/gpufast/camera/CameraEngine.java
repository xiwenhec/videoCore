package com.gpufast.camera;

import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.gpufast.logger.ELog;
import com.gpufast.render.Render;

/**
 * @author Sivin 2018/10/26
 * Description:Cmaera引擎
 */
public class CameraEngine implements SurfaceHolder.Callback {
    private static final String TAG = "CameraEngine";
    private static CameraEngine mInstance = null;
    private ICamera mICamera;
    //本地预览的
    private SurfaceHolder mSurfaceHolder;
    private Render mPreViewRender;
    //渲染回调
    private Render.OnRenderCallback mFrameCallback = null;

    private CameraEngine() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mICamera = new Camera19();
        } else {
            mICamera = new Camera21();
        }
    }

    public static CameraEngine getInstance() {
        if (mInstance == null) {
            synchronized (CameraEngine.class) {
                if (mInstance == null) {
                    mInstance = new CameraEngine();
                }
            }
        }
        return mInstance;
    }

    public void setPreview(SurfaceView view) {
        ELog.i(TAG, "setPreview");
        mSurfaceHolder = view.getHolder();
        mSurfaceHolder.addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPreViewRender = new Render(holder.getSurface());
        mPreViewRender.setOnRenderCallback(mFrameCallback);
        mPreViewRender.render();
        CameraParams params = new CameraParams.Builder()
                .setWidth(720)
                .setHeight(1280)
                .setTexture(mPreViewRender.getVideoTexture())
                .build();

        ELog.i(TAG, "setCameraParams");
        mICamera.setCameraParams(params);
        mICamera.openCamera(ICamera.CAMERA_FRONT);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mPreViewRender.onSizeChanged(width, height);
        mICamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mPreViewRender.destroy();
        mICamera.stopCamera();
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (mICamera != null) {
            mICamera.switchCamera();
        }
    }

    /**
     * 设置渲染回调
     *
     * @param callback callback
     */
    public void setRenderFrameCallback(Render.OnRenderCallback callback) {
        mFrameCallback = callback;
    }

    /**
     * 释放摄像机
     */
    public void release() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(this);
            mSurfaceHolder = null;
        }
        mInstance = null;
    }


}
