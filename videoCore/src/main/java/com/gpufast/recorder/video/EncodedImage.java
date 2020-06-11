package com.gpufast.recorder.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;


/**
 * 编码后的一帧数据
 */
public class EncodedImage {

    public enum FrameType {
        EmptyFrame(0),
        VideoFrameKey(3),
        VideoFrameDelta(4);
        private final int nativeIndex;

        FrameType(int nativeIndex) {
            this.nativeIndex = nativeIndex;
        }

        public int getNative() {
            return nativeIndex;
        }

        static FrameType fromNativeIndex(int nativeIndex) {
            for (FrameType type : FrameType.values()) {
                if (type.getNative() == nativeIndex) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown native frame type: " + nativeIndex);
        }
    }

    public final ByteBuffer buffer;

    /**
     * 编码视频的宽度
     */
    public final int encodedWidth;

    /**
     * 编码视频的高度
     */
    public final int encodedHeight;

    public final long captureTimeNs;

    public final FrameType frameType;

    /**
     * 视频旋转角度
     */
    public final int rotation;

    /**
     * 是否是一个完整的帧
     */
    public final boolean completeFrame;

    public final Integer qp;

    public MediaFormat mediaFormat;

    public MediaCodec.BufferInfo bufferInfo;

    public long startTime;

    public int index = 0;

    private EncodedImage(ByteBuffer buffer, int encodedWidth, int encodedHeight, long captureTimeNs,
                         FrameType frameType, int rotation, boolean completeFrame, Integer qp,
                         MediaFormat mediaFormat, MediaCodec.BufferInfo bufferInfo , int index) {
        this.buffer = buffer;
        this.encodedWidth = encodedWidth;
        this.encodedHeight = encodedHeight;
        this.captureTimeNs = captureTimeNs;
        this.frameType = frameType;
        this.rotation = rotation;
        this.completeFrame = completeFrame;
        this.qp = qp;
        this.mediaFormat = mediaFormat;
        this.bufferInfo = bufferInfo;
        this.index = index;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ByteBuffer buffer;
        private int encodedWidth;
        private int encodedHeight;
        private long captureTimeNs;
        private EncodedImage.FrameType frameType;
        private int rotation;
        private boolean completeFrame;
        private Integer qp;
        private MediaFormat mediaFormat;
        private MediaCodec.BufferInfo bufferInfo;
        private int frameIndex = 0;

        private Builder() {
        }

        public Builder setBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
            return this;
        }

        public Builder setEncodedWidth(int encodedWidth) {
            this.encodedWidth = encodedWidth;
            return this;
        }

        public Builder setEncodedHeight(int encodedHeight) {
            this.encodedHeight = encodedHeight;
            return this;
        }

        public Builder setCaptureTimeNs(long captureTimeNs) {
            this.captureTimeNs = captureTimeNs;
            return this;
        }

        public Builder setFrameType(EncodedImage.FrameType frameType) {
            this.frameType = frameType;
            return this;
        }

        public Builder setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder setCompleteFrame(boolean completeFrame) {
            this.completeFrame = completeFrame;
            return this;
        }

        public Builder setQp(Integer qp) {
            this.qp = qp;
            return this;
        }

        public Builder setFrameIndex(int index){
            frameIndex = index;
            return this;
        }

        /**
         * for IMediaMuxer
         *
         * @param bufferInfo
         * @return
         */
        public Builder setBufferInfo(MediaCodec.BufferInfo bufferInfo) {
            this.bufferInfo = bufferInfo;
            return this;
        }

        public EncodedImage createEncodedImage() {
            return new EncodedImage(buffer, encodedWidth, encodedHeight, captureTimeNs, frameType,
                    rotation, completeFrame, qp, mediaFormat, bufferInfo,frameIndex);
        }
    }

}
