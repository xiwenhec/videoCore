package com.gpufast.camera;

public interface ISurface {

    void register(SurfaceChangeListener listener);

    int getWidth();

    int getHeight();
}
