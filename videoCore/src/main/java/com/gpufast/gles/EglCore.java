/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.gpufast.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;

/**
 * Holds EGL state and utility methods for handling an egl 1.0 EGLContext, an EGLDisplay, and an EGLSurface.
 */
@SuppressWarnings("unused")
public interface EglCore {

    //根据文档，如果每一个线程都拥有自己的EGLContext对象，则EGL能用于多线程中，
    //但是在实际操作中，它会在某个设备上死锁，因此，在调用可能死锁的危险EGL函数之前同步此全局锁。
    Object lock = new Object();

    // These constants are taken from EGL14.EGL_OPENGL_ES2_BIT and EGL14.EGL_CONTEXT_CLIENT_VERSION.
    // https://android.googlesource.com/platform/frameworks/base/+/master/opengl/java/android/opengl/EGL14.java
    // This is similar to how GlSurfaceView does:
    // http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/5.1.1_r1/android/opengl/GLSurfaceView.java#760

    //
    int EGL_OPENGL_ES2_BIT = 4;
    // Android平台的特有扩展
    int EGL_RECORDABLE_ANDROID = 0x3142;

    // clang-format off
    int[] CONFIG_PLAIN = {  //简单配置
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL10.EGL_NONE
    };
    int[] CONFIG_RGBA = {
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_ALPHA_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL10.EGL_NONE
    };
    int[] CONFIG_PIXEL_BUFFER = {
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
        EGL10.EGL_NONE
    };
    int[] CONFIG_PIXEL_RGBA_BUFFER = {
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_ALPHA_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
        EGL10.EGL_NONE
    };
    int[] CONFIG_RECORDABLE = {
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //指定渲染API级别
        EGL_RECORDABLE_ANDROID, 1,
        EGL10.EGL_NONE
    };
    // clang-format on

    /**
     * Create a new context with the specified config attributes, sharing data with |sharedContext|. If |sharedContext|
     * is null, a root context is created. This function will try to create an EGL 1.4 context if possible, and an EGL
     * 1.0 context otherwise.
     */
    static EglCore create(EGLContext sharedContext, int[] configAttributes) {
        return new EglCore14(sharedContext, configAttributes);
    }

    /**
     * Helper function for creating a plain root context. This function will try to create an EGL 1.4 context if
     * possible, and an EGL 1.0 context otherwise.
     */
    static EglCore create() {
        return create(null, CONFIG_PLAIN);
    }

    /**
     * Helper function for creating a plain context, sharing data with |sharedContext|. This function will try to create
     * an EGL 1.4 context if possible, and an EGL 1.0 context otherwise.
     */
    static EglCore create(EGLContext sharedContext) {
        return create(sharedContext, CONFIG_PLAIN);
    }

    /**
     * Explicitly create a root EGl 1.4 context with the specified config attributes.
     */
    static EglCore createEgl14(int[] configAttributes) {
        return new EglCore14(null, configAttributes);
    }

    /**
     * Explicitly create a root EGl 1.4 context with the specified config attributes and shared context.
     */
    static EglCore createEgl14(
        EGLContext sharedContext, int[] configAttributes) {
        return new EglCore14(sharedContext, configAttributes);
    }

    void createSurface(Surface surface);

    // Create EGLSurface from the Android SurfaceTexture.
    void createSurface(SurfaceTexture surfaceTexture);

    // Create dummy 1x1 pixel buffer surface so the context can be made current.
    void createDummyPbufferSurface();

    void createPbufferSurface(int width, int height);

    EGLContext getEglContext();

    boolean hasSurface();

    int surfaceWidth();

    int surfaceHeight();

    void releaseSurface();

    void release();

    void makeCurrent();

    // Detach the current EGL context, so that it can be made current on another thread.
    void detachCurrent();

    void swapBuffers();

    void swapBuffers(long presentationTimeStampNs);
}
