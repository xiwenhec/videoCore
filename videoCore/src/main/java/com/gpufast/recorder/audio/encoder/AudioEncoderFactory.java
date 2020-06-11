package com.gpufast.recorder.audio.encoder;

public interface AudioEncoderFactory {

    /**
     * 设备可用的编码器信息集
     * @return 编码器信息集合
     */
    AudioCodecInfo getSupportCodecInfo();

    /**
     * 创建音频编码器
     * @param inputCodecInfo 编码器信息
     * @return 音频编码器对象
     */
    AudioEncoder createEncoder(AudioCodecInfo inputCodecInfo);

}
