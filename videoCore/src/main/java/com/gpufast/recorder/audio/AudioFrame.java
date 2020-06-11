package com.gpufast.recorder.audio;

import java.nio.ByteBuffer;

public class AudioFrame {

    public ByteBuffer buf;
    public int len;
    //时间戳（微秒）
    public long timeStamp_us;

    public AudioFrame(ByteBuffer buf, int len, long timeStamp_us) {
        this.buf = buf;
        this.len = len;
        this.timeStamp_us = timeStamp_us;
    }

}
