package com.gpufast.recorder.video;

import android.graphics.Matrix;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class VideoFrame {
    /**
     * 存储图像的媒介. 可能是openGL的 texture 或者是一块包含I420(yyyuuvv-yuv420p) data的内存数据
     */
    public interface Buffer {
        /**
         * Resolution of the buffer in pixels.
         */
        int getWidth();

        int getHeight();

        /**
         * 返回一个I420的内存数据，如果不是I420，则所有的实现类必须能够做到转换成I420。
         */
        I420Buffer toI420();

        void release();

        /**
         * 用指定的参数|cropx|, |cropY|, |cropWidth| and |cropHeight|，裁剪出一个区域。
         * 用指定的参数：|scaleWidth| x |scaleHeight|，进行缩放
         */
        Buffer cropAndScale(
                int cropX, int cropY, int cropWidth, int cropHeight, int scaleWidth, int scaleHeight);
    }


    public interface I420Buffer extends Buffer {
        /**
         * 返回一个direct ByteBuffer:包含Y-plane data; buffer的capacity至少是getStrideY() * getHeight()
         * 字节大小。buffer的position必须是0，调用者可能会改变这个buffer，因此实现者必须每次都要返回一个新的buffer
         */
        ByteBuffer getDataY();

        /**
         * 返回一个direct ByteBuffer:包含U-plane data;.
         * buffer的capacity至少是getStrideU() * ((getHeight() + 1) / 2) bytes.
         * buffer的position必须是0.
         * 调用者可能会改变这个buffer，因此实现者必须每次都要返回一个新的buffer
         */
        ByteBuffer getDataU();

        /**
         * 返回一个direct ByteBuffer:包含V-plane data;.
         * buffer的capacity至少是getStrideV() * ((getHeight() + 1) / 2) bytes.
         * buffer的position必须是0.
         * 调用者可能会改变这个buffer，因此实现者必须每次都要返回一个新的buffer
         */
        ByteBuffer getDataV();

        int getStrideY();

        int getStrideU();

        int getStrideV();
    }

    /**
     * Interface for buffers that are stored as a single texture, either in OES or RGB format.
     */
    public interface TextureBuffer extends Buffer {
        enum TextureType {
            OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
            RGB(GLES20.GL_TEXTURE_2D);
            private final int glTarget;
            TextureType(final int glTarget) {
                this.glTarget = glTarget;
            }
            public int getGlTarget() {
                return glTarget;
            }
        }

        TextureType getType();

        int getTextureId();
        /**
         * Retrieve the transform matrix associated with the frame. This transform matrix maps 2D
         * homogeneous coordinates of the form (s, t, 1) with s and t in the inclusive range [0, 1] to
         * the coordinate that should be used to sample that location from the buffer.
         */
        Matrix getTransformMatrix();
    }

    private final Buffer buffer;
    private final int rotation;
    private final long timestampNs;

    /**
     * Constructs a new VideoFrame backed by the given {@code buffer}.
     *
     * @note Ownership of the buffer object is tranferred to the new VideoFrame.
     */
    public VideoFrame(Buffer buffer, int rotation, long timestampNs) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer not allowed to be null");
        }
        if (rotation % 90 != 0) {
            throw new IllegalArgumentException("rotation must be a multiple of 90");
        }
        this.buffer = buffer;
        this.rotation = rotation;
        this.timestampNs = timestampNs;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    /**
     * Rotation of the frame in degrees.
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Timestamp of the frame in nano seconds.
     */
    public long getTimestampNs() {
        return timestampNs;
    }

    public int getRotatedWidth() {
        if (rotation % 180 == 0) {
            return buffer.getWidth();
        }
        return buffer.getHeight();
    }

    public int getRotatedHeight() {
        if (rotation % 180 == 0) {
            return buffer.getHeight();
        }
        return buffer.getWidth();
    }

    public void release() {
        buffer.release();
    }


}
