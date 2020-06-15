package com.gpufast.camera;

import android.view.Surface;

public interface SurfaceChangeListener {

    void onSurfaceCreate(Surface surface);

    void onSurfaceSizeChange(int width, int height);

    void onSurfaceDestroy(Surface surface);
}
