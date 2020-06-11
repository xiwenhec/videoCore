package com.gpufast.recorder;


import android.opengl.EGLContext;

import com.gpufast.recorder.audio.AudioProcessor;

public class RecorderEngine {
    private static IRecorder worker;

    private static IRecorder create() {
        if (worker == null) {
            synchronized (RecorderEngine.class) {
                if (worker == null) {
                    worker = new Worker();
                }
            }
        }
        return worker;
    }

    /**
     * 设置录制参数
     * @param params 参数
     */
    public static void setParams(RecordParams params) {
        create().setParams(params);
    }

    /**
     * 如果使用openGL传递信息，这需要传递EGLContext
     * @param eglContext EGL上下文
     */
    public static void setShareContext(EGLContext eglContext) {
        create().setShareContext(eglContext);
    }


    public static void setAudioProcessor(AudioProcessor callback){
        create().setAudioProcessor(callback);
    }


    /***
     * 送入视频数据给录音器
     * @param textureId 纹理Id
     * @param srcTexWidth 纹理的宽度
     * @param srcTexHeight 纹理的高度
     */
    public static void sendVideoFrame(int textureId, int srcTexWidth, int srcTexHeight) {
        create().sendVideoFrame(textureId,srcTexWidth,srcTexHeight);
    }

    /**
     * 是否正在录制
     * @return
     */
    public static boolean isRecording() {
        return create().isRecording();
    }


    public static void startRecorder() {
        create().startRecorder();
    }

    public static void jointVideo() {
        create().jointVideo();
    }

    public static void stopRecorder() {
        create().stopRecorder();
    }

    public void setRecorderListener(IRecorder.RecordListener listener) {
        create().setRecordListener(listener);
    }

    public static void release() {
        create().release();
        worker = null;
    }
}
