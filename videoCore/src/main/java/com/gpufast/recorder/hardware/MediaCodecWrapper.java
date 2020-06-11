package com.gpufast.recorder.hardware;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;

import java.nio.ByteBuffer;

public interface MediaCodecWrapper {
    void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags);

    void start();

    void flush();

    void stop();

    void release();

    int dequeueInputBuffer(long timeoutUs);

    void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags);

    int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs);

    void releaseOutputBuffer(int index, long renderTimestampNs);

    MediaFormat getOutputFormat();

    Surface createInputSurface();

    void setParameters(Bundle params);

    ByteBuffer getInputBuffer(int inputBufferIndex);

    ByteBuffer getOutputBuffer(int outputBufferIndex);
}