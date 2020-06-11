package com.gpufast.effect.filter;


import android.opengl.GLES20;

import com.gpufast.gles.GLESUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ImageFilter {


    private static final String VERTEX_SHADER = "attribute vec4 vPosition;\n" +
            "attribute vec4 vTexCoordinate;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main () {\n" +
            "    v_TexCoord = vTexCoordinate.xy;\n" +
            "    gl_Position = vPosition;\n" +
            "}";
    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform sampler2D texture;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main () {\n" +
            "    vec4 color = texture2D(texture, v_TexCoord);\n" +
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
    //纹理采样单元索引
    private int textureHandle;

    private int mOffscreenTextureId;
    private int mFramebuffer;

    public ImageFilter() {
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
        textureHandle = GLES20.glGetUniformLocation(mProgram, "texture");
        GLES20.glUseProgram(0);
    }


    public void onSizeChanged(int width ,int height){

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
        mOffscreenTextureId = values[0];   // expected > 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureId);
        GLESUtil.checkGlError("glBindTexture " + mOffscreenTextureId);

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
                GLES20.GL_TEXTURE_2D, mOffscreenTextureId, 0);
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



    /**
     * 绘制纹理
     * @param textureId 传入的纹理
     * @return 返回新的纹理id
     */
    public int drawTexture(int textureId) {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);


        GLES20.glEnableVertexAttribArray(textureCoordinateLoc);
        GLES20.glVertexAttribPointer(textureCoordinateLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(textureCoordinateLoc);
        GLES20.glUseProgram(0);
        return 0;
    }


    public void destroy() {
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
        }
        mProgram = 0;
    }

}
