package com.gpufast.recorder;

import com.gpufast.recorder.audio.encoder.AudioEncoderFactory;
import com.gpufast.recorder.audio.encoder.HwAudioEncoderFactory;
import com.gpufast.recorder.video.EncoderType;
import com.gpufast.recorder.video.VideoEncoderFactory;
import com.gpufast.recorder.video.encoder.HwVideoEncoderFactory;

/**
 * @author Sivin 2019/3/27
 * Description:
 */
class EncoderFactory {

    static VideoEncoderFactory getVideoEncoderFactory(EncoderType type) {
        switch (type) {
            case HW_VIDEO_ENCODER:
                return new HwVideoEncoderFactory();
            case SW_VIDEO_ENCODER:
                //TODO:编写视频软编码实现
                return null;
        }
        return null;
    }

    static AudioEncoderFactory getAudioEncoder(EncoderType type) {

        switch (type) {
            case HW_AUDIO_ENCODER:
                return new HwAudioEncoderFactory();
            case SW_AUDIO_ENCODER:
                //TODO:编写音频软编码实现
                return null;
        }
        return null;
    }

}
