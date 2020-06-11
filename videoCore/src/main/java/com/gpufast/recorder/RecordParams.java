package com.gpufast.recorder;


import android.media.AudioFormat;

import com.gpufast.logger.ELog;

public class RecordParams {

    private static final String TAG = "RecordParams";

    //视频码率(Kilobits per second）
    private final static int DEFAULT_VIDEO_BITRATE = 15000;
    //视频帧率
    private final static int DEFAULT_VIDEO_FRAME_RATE = 30;
    //音频采样率
    private final static int DEFAULT_AUDIO_SAMPLE_RATE = 44100;
    //音频码率bps
    private final static int DEFAULT_AUDIO_BITRATE = 64000;
    //默认麦克风声音输出格式
    private final static int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    //默认麦克风声音输入声音通道个数
    private final static int DEFAULT_AUDIO_CHANNELS = 1;

    public enum SpeedType {
        hyperslow, slow, standard, fast, veryFast
    }

    /**
     * 录制视频的高度
     */
    private int videoWidth;
    /**
     * 录制视频的宽度
     */
    private int videoHeight;

    /**
     * 录制的总时长
     */
    private int allTime;

    /**
     * 录制资源存放的路径
     */
    private String savePath;

    /**
     * 是否静音录制
     */
    private boolean muteMic;

    /**
     * 录制速度
     */
    private SpeedType speedType;

    /**
     * 背景音乐路径
     */
    private String backgroundMusicUrl;

    /**
     * 是否开启硬编码
     */
    private boolean enableHwEncoder;

    private RecordParams(Builder builder) {
        videoWidth = builder.videoWidth;
        videoHeight = builder.videoHeight;
        allTime = builder.allTime;
        savePath = builder.savePath;
        muteMic = builder.muteMic;
        speedType = builder.speedType;
        backgroundMusicUrl = builder.backgroundMusicUrl;
        enableHwEncoder = builder.enableHwEncoder;
        ELog.i(TAG, builder.toString());
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public int getAllTime() {
        return allTime;
    }

    public String getSavePath() {
        return savePath;
    }

    public boolean isMuteMic() {
        return muteMic;
    }


    public SpeedType getSpeedType() {
        return speedType;
    }

    public String getBackgroundMusicUrl() {
        return backgroundMusicUrl;
    }

    public boolean isEnableHwEncoder() {
        return enableHwEncoder;
    }

    public int getVideoBitrate() {
        return DEFAULT_VIDEO_BITRATE;
    }

    public int getVideoFrameRate() {
        return DEFAULT_VIDEO_FRAME_RATE;
    }

    public int getAudioSampleRate() {
        return DEFAULT_AUDIO_SAMPLE_RATE;
    }

    public int getAudioBitrate() {
        return DEFAULT_AUDIO_BITRATE;
    }

    public int getAudioRecordChannels() {
        return DEFAULT_AUDIO_CHANNELS;
    }

    public int getAduioRecordFormat() {
        return DEFAULT_AUDIO_FORMAT;
    }

    public static class Builder {
        private int videoWidth;
        private int videoHeight;
        private int allTime;
        private String savePath;
        private boolean muteMic = false;
        private SpeedType speedType;
        private String backgroundMusicUrl;
        private boolean enableHwEncoder = true;

        public Builder setVideoWidth(int videoWidth) {
            this.videoWidth = videoWidth;
            return this;
        }

        public Builder setVideoHeight(int videoHeight) {
            this.videoHeight = videoHeight;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Builder setEnableHwEncoder(boolean hwEncoder) {
            this.enableHwEncoder = hwEncoder;
            return this;
        }

        public Builder setBackgroundMusicUrl(String backgroundMusicUrl) {
            this.backgroundMusicUrl = backgroundMusicUrl;
            return this;
        }

        public Builder setSpeedType(SpeedType speedType) {
            this.speedType = speedType;
            return this;
        }

        public Builder setAllTime(int allTime) {
            this.allTime = allTime;
            return this;
        }
        public Builder setMuteMic(boolean muteMic) {
            this.muteMic = muteMic;
            return this;
        }

        public RecordParams build() {
            return new RecordParams(this);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "videoWidth=" + videoWidth +
                    ", videoHeight=" + videoHeight +
                    ", allTime=" + allTime +
                    ", savePath='" + savePath + '\'' +
                    ", muteMic=" + muteMic +
                    ", speedType=" + speedType +
                    ", backgroundMusicUrl='" + backgroundMusicUrl + '\'' +
                    ", enableHwEncoder=" + enableHwEncoder +
                    '}';
        }
    }

}