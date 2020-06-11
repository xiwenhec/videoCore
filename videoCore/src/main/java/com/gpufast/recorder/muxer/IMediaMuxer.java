package com.gpufast.recorder.muxer;


import com.gpufast.recorder.audio.encoder.AudioEncoder;
import com.gpufast.recorder.video.VideoEncoder;


/**
 * 视频复用器
 */
public abstract class IMediaMuxer implements VideoEncoder.VideoEncoderCallback,
        AudioEncoder.AudioEncoderCallback {

    static class Setting {
        String savePath;
        boolean muteMic;

        Setting(String savePath, boolean muteMic) {
            this.savePath = savePath;
            this.muteMic = muteMic;
        }
    }

    abstract void release();


}
