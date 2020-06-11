package com.gpufast.recorder.video.renderer;

import android.graphics.Matrix;
import android.graphics.Point;

import com.gpufast.recorder.video.VideoFrame;

public class VideoFrameDrawer {

    // These points are used to calculate the size of the part of the frame we are rendering.
    final static float[] srcPoints =
            new float[]{0f /* x0 */, 0f /* y0 */, 1f /* x1 */, 0f /* y1 */, 0f /* x2 */, 1f /* y2 */};
    private final float[] dstPoints = new float[6];
    private final Point renderSize = new Point();
    private int renderWidth;
    private int renderHeight;


    private final Matrix renderMatrix = new Matrix();


    public void drawFrame(VideoFrame frame, RendererCommon.GlDrawer drawer, Matrix additionalRenderMatrix) {

        drawFrame(frame, drawer, additionalRenderMatrix, 0 , 0 ,
                frame.getRotatedWidth(), frame.getRotatedHeight());
    }

    public void drawFrame(VideoFrame frame, RendererCommon.GlDrawer drawer,
                          Matrix additionalRenderMatrix, int viewportX, int viewportY, int viewportWidth,
                          int viewportHeight) {

        final int width = frame.getRotatedWidth();
        final int height = frame.getRotatedHeight();

        //计算应用矩阵后目标视频的宽度和高度
        calculateTransformedRenderSize(width, height, additionalRenderMatrix);

        final boolean isTextureFrame = frame.getBuffer() instanceof VideoFrame.TextureBuffer;

        renderMatrix.reset();

        //只针对I420
        renderMatrix.preTranslate(0.5f, 0.5f);
        if (!isTextureFrame) {
            renderMatrix.preScale(1f, -1f); // I420-frames are upside down
        }
        renderMatrix.preRotate(frame.getRotation());

        //前面设置过，这里恢复回来
        renderMatrix.preTranslate(-0.5f, -0.5f);

        if (additionalRenderMatrix != null) {
            renderMatrix.preConcat(additionalRenderMatrix);
        }

        drawTexture(drawer, (VideoFrame.TextureBuffer) frame.getBuffer(), renderMatrix, renderWidth,
                renderHeight, viewportX, viewportY, viewportWidth, viewportHeight);
    }


    // Calculate the frame size after |renderMatrix| is applied. Stores the output in member variables
    // |renderWidth| and |renderHeight| to avoid allocations since this function is called for every
    // frame.
    //计算矩阵变换完成之后，真的宽度和高度，最后通过回调返回给用户
    private void calculateTransformedRenderSize(
            int frameWidth, int frameHeight, Matrix renderMatrix) {
        if (renderMatrix == null) {
            renderWidth = frameWidth;
            renderHeight = frameHeight;
            return;
        }
        // Transform the texture coordinates (in the range [0, 1]) according to |renderMatrix|.
        renderMatrix.mapPoints(dstPoints, srcPoints);

        // Multiply with the width and height to get the positions in terms of pixels.
        for (int i = 0; i < 3; ++i) {
            dstPoints[i * 2 + 0] *= frameWidth;
            dstPoints[i * 2 + 1] *= frameHeight;
        }

        // Get the length of the sides of the transformed rectangle in terms of pixels.
        renderWidth = distance(dstPoints[0], dstPoints[1], dstPoints[2], dstPoints[3]);
        renderHeight = distance(dstPoints[0], dstPoints[1], dstPoints[4], dstPoints[5]);
    }


    /**
     * 返回两个点之间的距离
     */
    private static int distance(float x0, float y0, float x1, float y1) {
        return (int) Math.round(Math.hypot(x1 - x0, y1 - y0));
    }


    /**
     * Draws a VideoFrame.TextureBuffer. Calls either drawer.drawOes or drawer.drawRgb
     * depending on the type of the buffer. You can supply an additional render matrix. This is
     * used multiplied together with the transformation matrix of the frame. (M = renderMatrix *
     * transformationMatrix)
     */
    static void drawTexture(RendererCommon.GlDrawer drawer, VideoFrame.TextureBuffer buffer,
                            Matrix renderMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY,
                            int viewportWidth, int viewportHeight) {
        Matrix finalMatrix = new Matrix(buffer.getTransformMatrix());
        finalMatrix.preConcat(renderMatrix);
        float[] finalGlMatrix = RendererCommon.convertMatrixFromAndroidGraphicsMatrix(finalMatrix);
        switch (buffer.getType()) {
            case OES:
                drawer.drawOes(buffer.getTextureId(), finalGlMatrix, frameWidth, frameHeight, viewportX,
                        viewportY, viewportWidth, viewportHeight);
                break;
            case RGB:
                drawer.drawRgb(buffer.getTextureId(), finalGlMatrix, frameWidth, frameHeight, viewportX,
                        viewportY, viewportWidth, viewportHeight);
                break;
            default:
                throw new RuntimeException("Unknown texture type.");
        }
    }

    public void release(){}
}
