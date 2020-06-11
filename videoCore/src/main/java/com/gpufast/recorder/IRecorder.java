package com.gpufast.recorder;

import android.opengl.EGLContext;

import com.gpufast.recorder.audio.AudioProcessor;

public interface IRecorder {

    /**
     * Set the video recording parameter
     *
     * @param params {@linkplain RecordParams}
     */
    void setParams(RecordParams params);

    /**
     * 设置EGL共享上下文
     *
     * @param shareContext
     */
    void setShareContext(EGLContext shareContext);


    /**
     * 传递图像数据信息
     *
     * @param textureId textureId
     * @param srcWidth  srcWidth
     * @param srcHeight srcHeight
     */
    void sendVideoFrame(int textureId, int srcWidth, int srcHeight);


    /**
     * 开始录制
     */
    void startRecorder();

    /**
     * 停止录制
     */
    void stopRecorder();


    /**
     * 是否正在录制
     *
     * @return true:正在录制
     */
    boolean isRecording();

    /**
     * 拼接视频
     */
    void jointVideo();

    /**
     * 设置录制监听回调
     * @param listener listener
     */
    void setRecordListener(RecordListener listener);

    /**
     * 设置音频预处理
     * @param processor processor
     */
    void setAudioProcessor(AudioProcessor processor);

    void release();

    interface RecordListener {
        /**
         * 开始录制
         */
        void onRecordStart();

        /**
         * 录制进度
         * @param progress 录制时间(单位s)
         */
        void onProgress(long progress);

        /**
         * 停止录制
         */
        void onRecordStop();
    }
}
