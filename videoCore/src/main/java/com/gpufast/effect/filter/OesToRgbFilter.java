package com.gpufast.effect.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.gpufast.gles.GLESUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class OesToRgbFilter {

    private static final String VERTEX_SHADER = "attribute vec4 vPosition;\n" +
            "attribute vec4 vTexCoordinate;\n" +
            "uniform mat4 textureTransform;\n" +
            "varying vec2 v_TexCoordinate;\n" +
            "\n" +
            "void main () {\n" +
            "    v_TexCoordinate = (textureTransform * vTexCoordinate).xy;\n" +
            "    gl_Position = vPosition;\n" +
            "}";
    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES texture;\n" +
            "varying vec2 v_TexCoordinate;\n" +
            "\n" +
            "void main () {\n" +
            "    vec4 color = texture2D(texture, v_TexCoordinate);\n" +
            "    gl_FragColor = color;\n" +
            "}";


    private static float squareSize = 1.0f;

    private static float squareCoords[] = {
            -squareSize, squareSize,
            -squareSize, -squareSize,
            squareSize, -squareSize,
            squareSize, squareSize};

    private float textureCoords[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private int mProgram = 0;

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private ShortBuffer drawListBuffer;

    //顶点属性索引
    private int positionLoc;
    private int textureCoordinateLoc;
    //纹理矩阵索引
    private int textureTransformHandle;
    //纹理采样单元索引
    private int textureHandle;

    private int mFrameBufferTextureId = 0;
    private int mFramebuffer = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    public OesToRgbFilter() {
        setupBuffer();
    }


    private void setupBuffer() {
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);
    }

    /**
     * 这个函数调用在openGL线程中
     */
    public void init() {


        mProgram = GLESUtil.createProgram(VERTEX_SHADER,FRAGMENT_SHADER);
        GLES20.glUseProgram(mProgram);

        positionLoc = GLES20.glGetAttribLocation(mProgram, "vPosition");
        textureCoordinateLoc = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");

        textureTransformHandle = GLES20.glGetUniformLocation(mProgram, "textureTransform");
        textureHandle = GLES20.glGetUniformLocation(mProgram, "texture");
    }


    public void onSizeChanged(int width, int height) {
        if(width != mWidth || height != mHeight){
            mWidth = width;
            mHeight = height;
            destroyFrameBuffer();
            prepareFramebuffer(width, height);
        }
    }

    /**
     * 准备离屏缓冲区
     */
    private void prepareFramebuffer(int width, int height) {
        GLESUtil.checkGlError("prepareFramebuffer start");
        int[] values = new int[1];
        // 创建颜色缓冲区
        GLES20.glGenTextures(1, values, 0);
        GLESUtil.checkGlError("glGenTextures");
        mFrameBufferTextureId = values[0];   // expected > 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextureId);
        GLESUtil.checkGlError("glBindTexture " + mFrameBufferTextureId);

        // 设置texture存储
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //设置缓冲区参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLESUtil.checkGlError("glTexParameter");

        // 创建帧缓冲区
        GLES20.glGenFramebuffers(1, values, 0);
        GLESUtil.checkGlError("glGenFramebuffers");
        mFramebuffer = values[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        GLESUtil.checkGlError("glBindFramebuffer " + mFramebuffer);


        //将颜色缓冲区绑定到帧缓冲区中
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextureId, 0);
        GLESUtil.checkGlError("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        //切换回默认的缓冲区
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLESUtil.checkGlError("prepareFramebuffer done");
    }


    private void destroyFrameBuffer() {
        if (mFrameBufferTextureId != 0) {
            int[] value = new int[]{mFrameBufferTextureId};
            GLES20.glDeleteTextures(1, value, 0);
            mFrameBufferTextureId = 0;
        }
        if (mFramebuffer != 0) {
            int[] value = new int[]{mFramebuffer};
            GLES20.glDeleteFramebuffers(1, value, 0);
            mFramebuffer = 0;
        }
    }


    /**
     * 绘制纹理
     * @param textureId     传入的纹理
     * @param textureMatrix 纹理矩阵
     * @return 返回新的纹理id
     */
    public int drawTexture(int textureId, float[] textureMatrix) {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glEnableVertexAttribArray(textureCoordinateLoc);
        GLES20.glVertexAttribPointer(textureCoordinateLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glUniformMatrix4fv(textureTransformHandle, 1, false, textureMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(textureCoordinateLoc);
        GLES20.glUseProgram(0);

        //切换回默认缓冲区
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBufferTextureId;
    }


    public void destroy() {
        destroyFrameBuffer();
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
        }
        mProgram = 0;
    }


}
