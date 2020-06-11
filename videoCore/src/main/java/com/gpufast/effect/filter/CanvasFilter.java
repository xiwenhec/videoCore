package com.gpufast.effect.filter;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import com.gpufast.gles.GLESUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 实现Android水印贴纸，为用户提供Android标准的Canvas API
 */
public class CanvasFilter {

    private static final String VERTEX_SHADER = "attribute vec4 vPosition;\n" +
            "attribute vec4 vTexCoordinate;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main () {\n" +
            "    v_TexCoord = vec2(vTexCoordinate.x,1.0 - vTexCoordinate.y);\n" +
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

    private int mWidth;
    private int mHeight;


    private Bitmap bitmapBoard;

    private Canvas canvas;

    private int textureId;

    private CanvasPainter mCanvasPainter;

    public interface CanvasPainter {
        void draw(Canvas androidCanvas);
    }


    public CanvasFilter(CanvasPainter painter) {
        mCanvasPainter = painter;
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

        mProgram = GLESUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        GLES20.glUseProgram(mProgram);

        positionLoc = GLES20.glGetAttribLocation(mProgram, "vPosition");
        textureCoordinateLoc = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");
        textureHandle = GLES20.glGetUniformLocation(mProgram, "texture");
        textureId = createTextureId();

        GLES20.glUseProgram(0);

    }

    public int createTextureId() {
        int[] textures = new int[1];
        //生成纹理对象id,然后绑定设置
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);

        //设置纹理采样参数
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        //设置纹理S,T的拉伸方式
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        //解除绑定,等到使用的时候在绑定
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        return textures[0];
    }

    public void onSizeChanged(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            if (bitmapBoard != null) {
                bitmapBoard.recycle();
                bitmapBoard = null;
            }
            bitmapBoard = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmapBoard);
        }
    }


    private void updateImage() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        if (bitmapBoard != null) {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmapBoard, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        }
    }


    /**
     * 绘制纹理
     *
     * @return 返回新的纹理id
     */
    public int drawTexture() {
        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordinateLoc);
        GLES20.glVertexAttribPointer(textureCoordinateLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        updateTexture();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(textureCoordinateLoc);
        GLES20.glUseProgram(0);
        return 0;
    }

    private void updateTexture() {
        if (mCanvasPainter != null) {
            mCanvasPainter.draw(canvas);
        }
        updateImage();
    }


    public void destroy() {
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
        }
        mProgram = 0;
        if (bitmapBoard != null) {
            bitmapBoard.recycle();
            bitmapBoard = null;
        }
    }


}
