package com.gpufast.recorder.video.renderer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.gpufast.gles.GLESUtil;

import java.nio.FloatBuffer;

/**
 * Helper class to implement an instance of RendererCommon.GlDrawer that can accept multiple input
 * sources (OES, RGB, or YUV) using a generic fragment shader as input. The generic fragment shader
 * should sample pixel values from the function "sample" that will be provided by this class and
 * provides an abstraction for the input source type (OES, RGB, or YUV). The texture coordinate
 * variable name will be "tc" and the texture matrix in the vertex shader will be "tex_mat". The
 * simplest possible generic shader that just draws pixel from the frame unmodified looks like:
 * void main() {
 * gl_FragColor = sample(tc);
 * }
 * This class covers the cases for most simple shaders and generates the necessary boiler plate.
 * Advanced shaders can always implement RendererCommon.GlDrawer directly.
 */
class GlGenericDrawer implements RendererCommon.GlDrawer {
    /**
     * The different shader types representing different input sources. YUV here represents three
     * separate Y, U, V textures.
     */
    public static enum ShaderType {
        OES, RGB, YUV
    }

    /**
     * The shader callbacks is used to customize behavior for a GlDrawer. It provides a hook to set
     * uniform variables in the shader before a frame is drawn.
     */
    public static interface ShaderCallbacks {
        /**
         * This callback is called when a new shader has been compiled and created. It will be called
         * for the first frame as well as when the shader type is changed. This callback can be used to
         * do custom initialization of the shader that only needs to happen once.
         */
        void onNewShader(GlShader shader);

        /**
         * This callback is called before rendering a frame. It can be used to do custom preparation of
         * the shader that needs to happen every frame.
         */
        void onPrepareShader(GlShader shader, float[] texMatrix, int frameWidth, int frameHeight,
                             int viewportWidth, int viewportHeight);
    }

    private static final String INPUT_VERTEX_COORDINATE_NAME = "in_pos";
    private static final String INPUT_TEXTURE_COORDINATE_NAME = "in_tc";
    private static final String TEXTURE_MATRIX_NAME = "tex_mat";
    private static final String DEFAULT_VERTEX_SHADER_STRING = "varying vec2 tc;\n"
            + "attribute vec4 in_pos;\n"
            + "attribute vec4 in_tc;\n"
            + "uniform mat4 tex_mat;\n"
            + "void main() {\n"
            + "  gl_Position = in_pos;\n"
            + "  tc = (tex_mat * in_tc).xy;\n"
            + "}\n";


    private static final FloatBuffer FULL_RECTANGLE_BUFFER = GLESUtil.createFloatBuffer(new float[]{
            -1.0f, -1.0f, // Bottom left.
            1.0f, -1.0f, // Bottom right.
            -1.0f, 1.0f, // Top left.
            1.0f, 1.0f, // Top right.
    });


    private static final FloatBuffer FULL_RECTANGLE_TEXTURE_BUFFER =
            GLESUtil.createFloatBuffer(new float[]{
                    0.0f, 0.0f, // Bottom left.
                    1.0f, 0.0f, // Bottom right.
                    0.0f, 1.0f, // Top left.
                    1.0f, 1.0f, // Top right.
            });

    static String createFragmentShaderString(String genericFragmentSource, ShaderType shaderType) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (shaderType == ShaderType.OES) {
            stringBuilder.append("#extension GL_OES_EGL_image_external : require\n");
        }
        stringBuilder.append("precision mediump float;\n");
        stringBuilder.append("varying vec2 tc;\n");

        if (shaderType == ShaderType.YUV) {
            stringBuilder.append("uniform sampler2D y_tex;\n");
            stringBuilder.append("uniform sampler2D u_tex;\n");
            stringBuilder.append("uniform sampler2D v_tex;\n");

            // Add separate function for sampling texture.
            // yuv_to_rgb_mat is inverse of the matrix defined in YuvConverter.
            stringBuilder.append("vec4 sample(vec2 p) {\n");
            stringBuilder.append("  float y = texture2D(y_tex, p).r * 1.16438;\n");
            stringBuilder.append("  float u = texture2D(u_tex, p).r;\n");
            stringBuilder.append("  float v = texture2D(v_tex, p).r;\n");
            stringBuilder.append("  return vec4(y + 1.59603 * v - 0.874202,\n");
            stringBuilder.append("    y - 0.391762 * u - 0.812968 * v + 0.531668,\n");
            stringBuilder.append("    y + 2.01723 * u - 1.08563, 1);\n");
            stringBuilder.append("}\n");
            stringBuilder.append(genericFragmentSource);
        } else {
            final String samplerName = shaderType == ShaderType.OES ? "samplerExternalOES" : "sampler2D";
            stringBuilder.append("uniform ").append(samplerName).append(" tex;\n");

            // Update the sampling function in-place.
            stringBuilder.append(genericFragmentSource.replace("sample(", "texture2D(tex, "));
        }

        return stringBuilder.toString();
    }

    private final String genericFragmentSource;
    private final String vertexShader;
    private final ShaderCallbacks shaderCallbacks;
    private ShaderType currentShaderType;
    private GlShader currentShader;
    private int inPosLocation;
    private int inTcLocation;
    private int texMatrixLocation;

    public GlGenericDrawer(String genericFragmentSource, ShaderCallbacks shaderCallbacks) {
        this(DEFAULT_VERTEX_SHADER_STRING, genericFragmentSource, shaderCallbacks);
    }

    public GlGenericDrawer(String vertexShader, String genericFragmentSource, ShaderCallbacks shaderCallbacks) {
        this.vertexShader = vertexShader;
        this.genericFragmentSource = genericFragmentSource;
        this.shaderCallbacks = shaderCallbacks;
    }




    /**
     * Draw an OES texture frame with specified texture transformation matrix. Required resources are
     * allocated at the first call to this function.
     */
    @Override
    public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {

        //绘制前准备
        prepareShader(ShaderType.OES, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
        // Bind the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);
        // Draw the texture.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // Unbind the texture as a precaution.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    /**
     * Draw a RGB(A) texture frame with specified texture transformation matrix. Required resources
     * are allocated at the first call to this function.
     */
    @Override
    public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        prepareShader(ShaderType.RGB, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
        // Bind the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // Draw the texture.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // Unbind the texture as a precaution.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Draw a YUV frame with specified texture transformation matrix. Required resources are allocated
     * at the first call to this function.
     */
    @Override
    public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        prepareShader(
                ShaderType.YUV, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
        // Bind the textures.
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);
        }
        // Draw the textures.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // Unbind the textures as a precaution.
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private void prepareShader(ShaderType shaderType, float[] texMatrix, int frameWidth,
                               int frameHeight, int viewportWidth, int viewportHeight) {
        final GlShader shader;
        if (shaderType.equals(currentShaderType)) {
            // Same shader type as before, reuse exising shader.
            shader = currentShader;
        } else {
            // Allocate new shader.
            currentShaderType = shaderType;
            if (currentShader != null) {
                currentShader.release();
            }
            shader = createShader(shaderType);
            currentShader = shader;

            shader.useProgram();
            // Set input texture units.
            if (shaderType == ShaderType.YUV) {
                GLES20.glUniform1i(shader.getUniformLocation("y_tex"), 0);
                GLES20.glUniform1i(shader.getUniformLocation("u_tex"), 1);
                GLES20.glUniform1i(shader.getUniformLocation("v_tex"), 2);
            } else {
                GLES20.glUniform1i(shader.getUniformLocation("tex"), 0);
            }

            GLESUtil.checkGlError("Create shader");
            shaderCallbacks.onNewShader(shader);
            texMatrixLocation = shader.getUniformLocation(TEXTURE_MATRIX_NAME);
            inPosLocation = shader.getAttribLocation(INPUT_VERTEX_COORDINATE_NAME);
            inTcLocation = shader.getAttribLocation(INPUT_TEXTURE_COORDINATE_NAME);
        }

        shader.useProgram();

        // Upload the vertex coordinates.
        GLES20.glEnableVertexAttribArray(inPosLocation);
        GLES20.glVertexAttribPointer(inPosLocation, 2, GLES20.GL_FLOAT, false, 0, FULL_RECTANGLE_BUFFER);

        GLES20.glEnableVertexAttribArray(inTcLocation);
        GLES20.glVertexAttribPointer(inTcLocation, 2, GLES20.GL_FLOAT, false, 0, FULL_RECTANGLE_TEXTURE_BUFFER);
        GLES20.glUniformMatrix4fv(texMatrixLocation, 1, false, texMatrix, 0);

        shaderCallbacks.onPrepareShader(shader, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
        GLESUtil.checkGlError("Prepare shader");
    }

    private GlShader createShader(ShaderType shaderType) {
        return new GlShader(vertexShader, createFragmentShaderString(genericFragmentSource, shaderType));
    }

    /**
     * Release all GLES resources. This needs to be done manually, otherwise the resources are leaked.
     */
    @Override
    public void release() {
        if (currentShader != null) {
            currentShader.release();
            currentShader = null;
            currentShaderType = null;
        }
    }
}
