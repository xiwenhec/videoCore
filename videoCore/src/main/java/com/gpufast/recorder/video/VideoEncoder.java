package com.gpufast.recorder.video;

import android.media.MediaFormat;

import com.gpufast.recorder.video.encoder.VideoCodecStatus;

public interface VideoEncoder {

    class Settings {
        //宽度高度
        public final int width;
        public final int height;
        //起始码率
        public final int startBitrate; // Kilobits per second.
        //最大帧率
        public final int maxFrameRate;

        public Settings(int width, int height,
                        int startBitrate, int maxFrameRate) {
            this.width = width;
            this.height = height;
            this.startBitrate = startBitrate;
            this.maxFrameRate = maxFrameRate;
        }
    }

    default boolean isHardwareEncoder() {
        return true;
    }

    //初始化编码器
    VideoCodecStatus init(Settings settings, VideoEncoderCallback encodeCallback);

    VideoCodecStatus encode(VideoFrame frame);

    String getImplementationName();

    VideoCodecStatus deInit();

    interface VideoEncoderCallback {

        void onUpdateVideoMediaFormat(MediaFormat format);

        void onEncodedFrame(EncodedImage frame);

        void onVideoEncoderStop();
    }
}
