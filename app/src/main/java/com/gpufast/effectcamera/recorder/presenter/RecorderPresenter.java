package com.gpufast.effectcamera.recorder.presenter;

import android.opengl.EGLContext;
import android.view.SurfaceView;

import com.gpufast.camera.CameraEngine;
import com.gpufast.effectcamera.recorder.contract.RecorderContract;
import com.gpufast.logger.ELog;
import com.gpufast.recorder.RecorderEngine;
import com.gpufast.recorder.RecordParams;
import com.gpufast.recorder.audio.AudioFrame;
import com.gpufast.recorder.audio.AudioProcessor;
import com.gpufast.render.Render;

public class RecorderPresenter implements RecorderContract.Presenter, Render.OnRenderCallback, AudioProcessor {
    private static final String TAG = "RecorderPresenter";

    private RecorderContract.View mView;
    private CameraEngine mCameraEngine;


    public void attachView(RecorderContract.View view) {
        mView = view;
    }


    public void init() {
        SurfaceView preview = mView.getPreview();
        if (preview == null) {
            ELog.e(TAG, "preview == null : true");
            return;
        }
        mCameraEngine = CameraEngine.getInstance();
        mCameraEngine.setPreview(preview);
        mCameraEngine.setRenderFrameCallback(this);
        RecorderEngine.setAudioProcessor(this);
    }

    @Override
    public void switchCamera() {
        mCameraEngine.switchCamera();
    }

    @Override
    public void onEglContextCreate(EGLContext eglContext) {
        RecorderEngine.setShareContext(eglContext);
    }


    @Override
    public int onFrameCallback(int textureId, int width, int height) {
        RecorderEngine.sendVideoFrame(textureId, width, height);
        return 0;
    }

    @Override
    public void onEglContextDestroy() {
    }


    @Override
    public void setRecorderParameter(RecordParams params) {
        RecorderEngine.setParams(params);
    }

    @Override
    public void startRecorder() {
        RecorderEngine.startRecorder();
    }

    @Override
    public void stopRecorder() {
        RecorderEngine.stopRecorder();
    }

    @Override
    public void jointVideo() {
        RecorderEngine.jointVideo();
    }

    @Override
    public boolean isRecording() {
        return RecorderEngine.isRecording();
    }


    @Override
    public AudioFrame onReceiveAudioFrame(AudioFrame frame) {
        //对声音进行编码前预处理
        return null;
    }
}
