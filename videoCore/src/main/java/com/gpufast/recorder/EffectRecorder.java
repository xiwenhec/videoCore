package com.gpufast.recorder;

import android.opengl.EGLContext;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.audio.AudioClient;
import com.gpufast.recorder.muxer.IMediaMuxer;
import com.gpufast.recorder.muxer.MediaMuxerFactory;
import com.gpufast.recorder.muxer.MuxerType;
import com.gpufast.recorder.video.VideoClient;

public class EffectRecorder extends BaseRecorder {
    private static final String TAG = EffectRecorder.class.getSimpleName();

    private volatile boolean recordStarting = false;
    private volatile boolean recordStarted = false;
    private VideoClient mVideoClient;
    private AudioClient mAudioClient;
    private RecordParams mRecordParams;
    private RecordListener mRecordListener;

    EffectRecorder() {
    }

    @Override
    public void setParams(RecordParams params) {
        if (params == null) {
            return;
        }
        initVideoParams(params);
        if (!params.isMuteMic()) {
            initAudioParams(params);
        }
        mRecordParams = params;
    }

    @Override
    public void setShareContext(EGLContext shareContext) {
        setEGLShareContext(shareContext);
    }

    @Override
    public boolean isRecording() {
        return recordStarted;
    }

    @Override
    public void startRecorder() {
        if (recordStarting || recordStarted) {
            return;
        }
        ELog.i(TAG, "startRecorder");
        recordStarting = true;
        IMediaMuxer mediaMuxer = MediaMuxerFactory.createMediaMuxer(mRecordParams, MuxerType.MP4);
        mVideoClient = createVideoClient(mediaMuxer);
        if (mVideoClient != null) {
            mVideoClient.start();
        }
        mAudioClient = createAudioClient(mediaMuxer);
        if (mAudioClient != null) {
            if (mAudioProcessor != null) {
                mAudioClient.setAudioPreprocessor(mAudioProcessor);
            }
            mAudioClient.start();
        }
        if (mRecordListener != null) {
            mRecordListener.onRecordStart();
        }
        recordStarted = true;
    }


    @Override
    public void sendVideoFrame(int textureId, int srcWidth, int srcHeight) {
        if (mVideoClient != null && recordStarted) {
            mVideoClient.sendVideoFrame(textureId, srcWidth, srcHeight);
        }
    }


    @Override
    public void stopRecorder() {
        ELog.i(TAG, "do stop recorder");
        recordStarted = false;
        if (mVideoClient != null) {
            mVideoClient.stop();
        }
        if (mAudioClient != null) {
            mAudioClient.stop();
        }
        recordStarting = false;
        ELog.i(TAG, "stop recorder finish");
        if (mRecordListener != null) {
            mRecordListener.onRecordStop();
        }
    }


    @Override
    public void jointVideo() {

    }


    @Override
    public void setRecordListener(RecordListener listener) {
        this.mRecordListener = listener;
    }


    @Override
    public void release() {

    }


}
