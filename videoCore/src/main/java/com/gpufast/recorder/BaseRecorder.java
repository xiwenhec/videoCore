package com.gpufast.recorder;

import android.opengl.EGLContext;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.audio.AudioClient;
import com.gpufast.recorder.audio.AudioProcessor;
import com.gpufast.recorder.audio.AudioSetting;
import com.gpufast.recorder.audio.encoder.AudioCodecInfo;
import com.gpufast.recorder.audio.encoder.AudioEncoder;
import com.gpufast.recorder.audio.encoder.AudioEncoderFactory;
import com.gpufast.recorder.muxer.IMediaMuxer;
import com.gpufast.recorder.video.EncoderType;
import com.gpufast.recorder.video.VideoClient;
import com.gpufast.recorder.video.VideoEncoder;
import com.gpufast.recorder.video.VideoEncoderFactory;
import com.gpufast.recorder.video.encoder.VideoCodecInfo;

public abstract class BaseRecorder implements IRecorder {
    private static final String TAG = "BaseRecorder";


    private EGLContext shareContext;

    private VideoEncoderFactory videoEncoderFactory;

    private VideoCodecInfo videoCodecInfo;

    private VideoEncoder.Settings videoSettings;


    private AudioSetting audioSetting;

    private AudioEncoderFactory audioEncoderFactory;

    private AudioCodecInfo audioCodecInfo;


    protected AudioProcessor mAudioProcessor;

    @Override
    public void setAudioProcessor(AudioProcessor processor) {
        mAudioProcessor = processor;
    }


    /**
     * 初始化视频录制
     *
     * @param params 录音器参数
     */
    void initVideoParams(RecordParams params) {
        videoSettings = new VideoEncoder.Settings(params.getVideoWidth(),
                params.getVideoHeight(), params.getVideoBitrate(), params.getVideoFrameRate());

        if (params.isEnableHwEncoder()) {
            videoEncoderFactory = EncoderFactory.getVideoEncoderFactory(EncoderType.HW_VIDEO_ENCODER);
        } else {
            videoEncoderFactory = EncoderFactory.getVideoEncoderFactory(EncoderType.SW_VIDEO_ENCODER);
        }
        if (videoEncoderFactory != null) {
            if (shareContext != null) {
                videoEncoderFactory.setShareContext(shareContext);
            }
            VideoCodecInfo[] supportedCodecs = videoEncoderFactory.getSupportedCodecs();
            if (supportedCodecs != null && supportedCodecs.length > 0) {
                videoCodecInfo = supportedCodecs[0];
                ELog.d(TAG, "find a codec :" + videoCodecInfo.name);
            } else {
                ELog.e(TAG, "can't find a available codec :");
            }
        }
    }


    void setEGLShareContext(EGLContext context) {
        shareContext = context;
        if (videoEncoderFactory != null) {
            videoEncoderFactory.setShareContext(shareContext);
        }
    }

    VideoClient createVideoClient(IMediaMuxer muxer) {
        if (videoEncoderFactory != null && videoCodecInfo != null) {
            VideoEncoder videoEncoder = videoEncoderFactory.createEncoder(videoCodecInfo);
            if (videoEncoder != null) {
                return new VideoClient(videoEncoder, videoSettings, muxer);
            }
            ELog.e(TAG, "can't create video encoder.");
        }
        ELog.e(TAG, "create video client failed.");
        return null;
    }


    void initAudioParams(RecordParams params) {
        audioSetting = new AudioSetting(
                params.getAudioSampleRate(),
                params.getAudioBitrate(),
                params.getAudioRecordChannels(),
                params.getAduioRecordFormat());
        if (params.isEnableHwEncoder()) {
            audioEncoderFactory = EncoderFactory.getAudioEncoder(EncoderType.HW_AUDIO_ENCODER);
        } else {
            audioEncoderFactory = EncoderFactory.getAudioEncoder(EncoderType.SW_AUDIO_ENCODER);
        }
        if (audioEncoderFactory != null) {
            audioCodecInfo = audioEncoderFactory.getSupportCodecInfo();
        }
    }

    AudioClient createAudioClient(IMediaMuxer muxer) {
        if (audioEncoderFactory != null && audioCodecInfo != null) {
            AudioEncoder audioEncoder = audioEncoderFactory.createEncoder(audioCodecInfo);
            if (audioEncoder != null) {
                return new AudioClient(audioEncoder, audioSetting, muxer);
            }
            ELog.e(TAG, "create audio encoder");
        }
        ELog.e(TAG, "create audio client failed.");
        return null;
    }
}
