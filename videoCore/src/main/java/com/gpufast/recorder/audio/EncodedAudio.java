package com.gpufast.recorder.audio;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class EncodedAudio {

    public final ByteBuffer buffer;
    public final MediaCodec.BufferInfo bufferInfo;

    private EncodedAudio(ByteBuffer mBuffer, MediaCodec.BufferInfo mBufferInfo) {
        this.buffer = mBuffer;
        this.bufferInfo = mBufferInfo;
    }


    public static class Builder{
        private ByteBuffer buffer;
        private MediaCodec.BufferInfo bufferInfo;

        public Builder() {
        }

        public Builder setBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
            return this;
        }



        public Builder setBufferInfo(MediaCodec.BufferInfo bufferInfo) {
            this.bufferInfo = bufferInfo;
            return this;
        }

        public EncodedAudio createEncodedAudio() {
            return new EncodedAudio(buffer,bufferInfo);
        }
    }
}
