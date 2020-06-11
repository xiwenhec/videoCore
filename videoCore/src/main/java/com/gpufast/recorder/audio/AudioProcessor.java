package com.gpufast.recorder.audio;

/**
 * 音频回调接收器
 */
public interface AudioProcessor {

    AudioFrame onReceiveAudioFrame(AudioFrame frame);
}
