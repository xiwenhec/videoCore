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
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;
import androidx.annotation.Nullable;

/**
 * Holds EGL state and utility methods for handling an EGL14 EGLContext, an EGLDisplay, and an EGLSurface.
 */
public class EglCore14 implements EglCore {
    private EGLContext mEglContext;
    @Nullable
    private EGLConfig mEglConfig;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

    @SuppressWarnings("all")
    public EglCore14(EGLContext sharedContext, int[] configAttributes) {
        mEglDisplay = getEglDisplay();
        mEglConfig = getEglConfig(mEglDisplay, configAttributes);
        mEglContext = createEglContext(sharedContext, mEglDisplay, mEglConfig);
    }

    @Override
    public void createSurface(Surface surface) {
        createSurfaceInternal(surface);
    }

    @Override
    public void createSurface(SurfaceTexture surfaceTexture) {
        createSurfaceInternal(surfaceTexture);
    }

    // Create EGLSurface from either Surface or SurfaceTexture.
    private void createSurfaceInternal(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new IllegalStateException("Input must be either a Surface or SurfaceTexture");
        }
        checkIsNotReleased();
        if (mEglSurface != EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("Already has an EGLSurface");
        }
        int[] surfaceAttribs = {EGL14.EGL_NONE};
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surfaceAttribs, 0);
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException(
                "Failed to create window surface: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    @Override
    public void createDummyPbufferSurface() {
        createPbufferSurface(1, 1);
    }

    @Override
    public void createPbufferSurface(int width, int height) {
        checkIsNotReleased();
        if (mEglSurface != EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("Already has an EGLSurface");
        }
        int[] surfaceAttribs = {EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE};
        mEglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0);
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("Failed to create pixel buffer surface with size " + width + "x"
                                           + height + ": 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    @Override
    public EGLContext getEglContext() {
        return mEglContext;
    }

    @Override
    public boolean hasSurface() {
        return mEglSurface != EGL14.EGL_NO_SURFACE;
    }

    @Override
    public int surfaceWidth() {
        final int[] widthArray = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, mEglSurface, EGL14.EGL_WIDTH, widthArray, 0);
        return widthArray[0];
    }

    @Override
    public int surfaceHeight() {
        final int[] heightArray = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, mEglSurface, EGL14.EGL_HEIGHT, heightArray, 0);
        return heightArray[0];
    }

    @Override
    public void releaseSurface() {
        if (mEglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    private void checkIsNotReleased() {
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY || mEglContext == EGL14.EGL_NO_CONTEXT
            || mEglConfig == null) {
            throw new RuntimeException("This object has been released");
        }
    }

    @Override
    public void release() {
        checkIsNotReleased();
        releaseSurface();
        detachCurrent();
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        EGL14.eglReleaseThread();
        EGL14.eglTerminate(mEglDisplay);
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglConfig = null;
    }

    @Override
    public void makeCurrent() {
        checkIsNotReleased();
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("No EGLSurface - can't make current");
        }
        synchronized (EglCore.lock) {
            if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                throw new RuntimeException(
                    "eglMakeCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
        }
    }

    // Detach the current EGL context, so that it can be made current on another thread.
    @Override
    public void detachCurrent() {
        synchronized (EglCore.lock) {
            if (!EGL14.eglMakeCurrent(
                mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException(
                    "eglDetachCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
        }
    }

    @Override
    public void swapBuffers() {
        checkIsNotReleased();
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("No EGLSurface - can't swap buffers");
        }
        synchronized (EglCore.lock) {
            EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
        }
    }

    @Override
    public void swapBuffers(long timeStampNs) {
        checkIsNotReleased();
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("No EGLSurface - can't swap buffers");
        }
        synchronized (EglCore.lock) {
            // See
            // https://android.googlesource.com/platform/frameworks/native/+/tools_r22.2/opengl/specs/EGL_ANDROID_presentation_time.txt
            EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, timeStampNs);
            EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
        }
    }

    // Return an EGLDisplay, or die trying.
    private static EGLDisplay getEglDisplay() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException(
                "Unable to get EGL14 display: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException(
                "Unable to initialize EGL14: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglDisplay;
    }

    // Return an EGLConfig, or die trying.
    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] configAttributes) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(
            eglDisplay, configAttributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException(
                "eglChooseConfig failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Unable to find any matching EGL config");
        }
        final EGLConfig eglConfig = configs[0];
        if (eglConfig == null) {
            throw new RuntimeException("eglChooseConfig returned null");
        }
        return eglConfig;
    }

    // Return an EGLConfig, or die trying.
    private static EGLContext createEglContext(
            @Nullable EGLContext sharedContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        if (sharedContext != null && sharedContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Invalid sharedContext");
        }
        int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        EGLContext rootContext =
            sharedContext == null ? EGL14.EGL_NO_CONTEXT : sharedContext;
        final EGLContext eglContext;
        synchronized (EglCore.lock) {
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, rootContext, contextAttributes, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException(
                "Failed to create EGL context: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglContext;
    }
}
