package com.gpufast.recorder.video;

import android.opengl.EGLContext;

import com.gpufast.recorder.video.encoder.VideoCodecInfo;


public interface VideoEncoderFactory {

    void setShareContext(EGLContext shareContext);

    VideoCodecInfo[] getSupportedCodecs();

    VideoEncoder createEncoder(VideoCodecInfo inputCodecInfo);

}
