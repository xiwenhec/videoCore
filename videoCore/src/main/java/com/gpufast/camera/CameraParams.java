package com.gpufast.camera;

import android.graphics.SurfaceTexture;

/**
 * @author Sivin 2018/11/27
 * Description:Cameara配置类，该类对象将传递到ICameara实现类中，用于配置Camera
 */
class CameraParams {

    private SurfaceTexture mPreTexture;

    private int width;

    private int height;

    private int fps;

    private CameraParams() {}

    private CameraParams(Builder builder) {
        mPreTexture = builder.texture;
        width = builder.width;
        height = builder.height;
        fps = builder.fps;
    }

    SurfaceTexture getPreTexture() {
        return mPreTexture;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    public int getFps() {
        return fps;
    }


    public static class Builder {
        private SurfaceTexture texture;
        private int width;
        private int height;
        private int fps;

        public CameraParams build() {
            if (texture == null) {
                throw new RuntimeException("A CameraParams must be set a texture");
            }
            return new CameraParams(this);
        }

        public Builder setTexture(SurfaceTexture texture) {
            this.texture = texture;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }
    }
}
