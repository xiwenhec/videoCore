package com.gpufast.recorder.hardware;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecWrapperFactoryImpl implements MediaCodecWrapperFactory {
    private static class MediaCodecWrapperImpl implements MediaCodecWrapper {
        private final MediaCodec mediaCodec;

        public MediaCodecWrapperImpl(MediaCodec mediaCodec) {
            this.mediaCodec = mediaCodec;
        }

        @Override
        public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
            mediaCodec.configure(format, surface, crypto, flags);
        }

        @Override
        public void start() {
            mediaCodec.start();
        }

        @Override
        public void flush() {
            mediaCodec.flush();
        }

        @Override
        public void stop() {
            mediaCodec.stop();
        }

        @Override
        public void release() {
            mediaCodec.release();
        }

        @Override
        public int dequeueInputBuffer(long timeoutUs) {
            return mediaCodec.dequeueInputBuffer(timeoutUs);
        }

        @Override
        public void queueInputBuffer(
                int index, int offset, int size, long presentationTimeUs, int flags) {
            mediaCodec.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
        }

        @Override
        public int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs) {
            return mediaCodec.dequeueOutputBuffer(info, timeoutUs);
        }

        @Override
        public void releaseOutputBuffer(int index, long renderTimestampNs) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaCodec.releaseOutputBuffer(index, renderTimestampNs);
            } else {
                mediaCodec.releaseOutputBuffer(index, renderTimestampNs != 0);
            }
        }

        @Override
        public MediaFormat getOutputFormat() {
            return mediaCodec.getOutputFormat();
        }

        @Override
        public ByteBuffer getOutputBuffer(int index) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return mediaCodec.getOutputBuffer(index);
            } else {
                return mediaCodec.getOutputBuffers()[index];
            }
        }

        @Override
        public Surface createInputSurface() {
            return mediaCodec.createInputSurface();
        }

        @Override
        public void setParameters(Bundle params) {
            mediaCodec.setParameters(params);
        }

        @Override
        public ByteBuffer getInputBuffer(int inputBufferIndex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return mediaCodec.getInputBuffer(inputBufferIndex);
            } else {
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                return inputBuffers[inputBufferIndex];
            }
        }

    }

    @Override
    public MediaCodecWrapper createByCodecName(String name) throws IOException {
        return new MediaCodecWrapperImpl(MediaCodec.createByCodecName(name));
    }
}