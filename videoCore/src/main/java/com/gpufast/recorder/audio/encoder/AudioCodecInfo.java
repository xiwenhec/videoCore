package com.gpufast.recorder.audio.encoder;

/**
 * 音频编码器描述信息
 */
public class AudioCodecInfo {
    AudioCodecType type;
    String name;

    public AudioCodecInfo(String name, AudioCodecType type) {
        this.name = name;
        this.type = type;
    }
}
