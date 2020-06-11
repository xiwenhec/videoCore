package com.gpufast.recorder.video.encoder;

/**
 * 视频编解码器的类型
 */
public enum VideoCodecType {

    H264("video/avc"),
    H265("video/hevc"),
    VP8("video/x-vnd.on2.vp8"),
    VP9("video/x-vnd.on2.vp9");

    private final String mimeType;

    VideoCodecType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }
}