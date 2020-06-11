package com.gpufast.recorder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;

/**
 * 音频采集参数，编码参数设置类
 */
public class AudioSetting {
    //音频码率，64000,96000,128000
    private final int bitrate;
    //音频采样率，如：48000 ，44100, 16000
    private final int sampleRate;

    //音频通道配置
    private int channelConfig;

    //声音输出的数据格式
    private final int audioFormat;

    //通道个数
    private final int channels;

    //麦克风最小输出buffer大小
    private final int inputBufferSize;

    /**
     * @param sampleRate  采样率44100
     * @param bitrate     码率64000
     * @param channels    通道个数 value 可选 1 或者 2
     * @param audioFormat 录音返回的数据格式
     *                    See {@link AudioFormat#ENCODING_PCM_8BIT}, {@link AudioFormat#ENCODING_PCM_16BIT},
     *                    and {@link AudioFormat#ENCODING_PCM_FLOAT}.
     */
    public AudioSetting(int sampleRate, int bitrate, int channels, int audioFormat) {
        this.sampleRate = sampleRate;
        this.bitrate = bitrate;
        this.channels = channels;
        if (channels == 1) {
            channelConfig = AudioFormat.CHANNEL_IN_MONO;
        }
        if (channels == 2) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        }
        this.audioFormat = audioFormat;

        inputBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getInputBufferSize() {
        return inputBufferSize;
    }

    public int getChannels() {
        return channels;
    }
}
