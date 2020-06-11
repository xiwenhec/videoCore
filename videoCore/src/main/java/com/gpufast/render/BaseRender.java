package com.gpufast.render;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.gpufast.gles.EglCore;
import com.gpufast.logger.ELog;

import java.lang.ref.WeakReference;


public abstract class BaseRender {
    public static String TAG = BaseRender.class.getSimpleName();

    private RenderThread mRenderThread;

    BaseRender(Surface surface) {
        onRenderInit();
        RenderCallback callback = getRenderCallback();
        mRenderThread = new RenderThread(surface, callback);
    }

    public void render() {
        mRenderThread.start();
        mRenderThread.waitUntilReady();
    }

    public boolean isReady() {
        return mRenderThread.isReady();
    }

    protected abstract void onRenderInit();


    public void onSizeChanged(int width, int height) {
        mRenderThread.getHandler().sendFrameSizeChanged(width, height);
    }

    public void destroy() {
        mRenderThread.getHandler().sendShutdown();
    }

    void onFrameAvailable() {
        if (mRenderThread.mReady) {
            mRenderThread.getHandler().sendFrameAvailable();
        }
    }

    protected abstract RenderCallback getRenderCallback();


    public EGLContext getEGLContext() {
        return mRenderThread.getEglContext();
    }


    private static class RenderThread extends Thread {
        private final Object mStartLock = new Object();
        private boolean mReady = false;
        private Surface surface;
        private RenderCallback callback;
        private RenderHandler mHandler;
        private EglCore mEglCore;

        boolean isReady() {
            return mReady;
        }

        private EGLContext getEglContext() {
            return mEglCore.getEglContext();
        }

        RenderHandler getHandler() {
            return mHandler;
        }

        RenderThread(Surface surface, RenderCallback cb) {
            this.surface = surface;
            callback = cb;
        }


        @Override
        public void run() {
            Looper.prepare();
            mHandler = new RenderHandler(this);
            mEglCore = EglCore.create();
            mEglCore.createSurface(surface);
            mEglCore.makeCurrent();

            callback.onInit();

            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();  //通知调用线程，渲染线程准备工作完成
            }
            //开始循环
            Looper.loop();
            mReady = false;
            callback.onDestroy();
            releaseEGL();
        }

        private void releaseEGL() {
            mEglCore.release();
        }

        /**
         * 可以在其他线程调用
         * 函数渲染线程调用，使渲染线程等待编码渲染宣城准备完毕
         */
        void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        private void onSizeChanged(int width, int height) {
            callback.onSizeChanged(width, height);
        }

        private void onFrameAvailable() {
            callback.onDraw();
            mEglCore.swapBuffers();
        }


        /**
         * 必须在当前线程调用
         */
        private void shutdown() {
            Log.d(TAG, "shutdown");
            Looper.myLooper().quit();
        }
    }


    private static class RenderHandler extends Handler {

        private static final int ON_FRAME_AVAILABLE = 0;
        private static final int ON_FRAME_SIZE_CHANGED = 1;
        private static final int ON_RENDER_SHUTDOWN = 2;
        private static final int ON_START_CAPTURE = 3;

        private WeakReference<RenderThread> mWeakRenderThread;

        void sendFrameAvailable() {
            sendMessage(obtainMessage(ON_FRAME_AVAILABLE));
        }

        void sendFrameSizeChanged(int width, int height) {
            sendMessage(obtainMessage(ON_FRAME_SIZE_CHANGED, width, height));
        }

        void sendShutdown() {
            sendMessage(obtainMessage(ON_RENDER_SHUTDOWN));
        }

        void setOnStartCapture() {
            sendMessage(obtainMessage(ON_START_CAPTURE));
        }

        RenderHandler(RenderThread thread) {
            mWeakRenderThread = new WeakReference<>(thread);
        }

        @Override
        public void handleMessage(Message msg) {
            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                ELog.e(TAG, "EncoderRenderHandler.handleMessage: weak ref is null");
                return;
            }
            switch (msg.what) {
                case ON_FRAME_AVAILABLE:
                    renderThread.onFrameAvailable();
                    break;
                case ON_FRAME_SIZE_CHANGED:
                    renderThread.onSizeChanged(msg.arg1, msg.arg2);
                    break;
                case ON_RENDER_SHUTDOWN:
                    renderThread.shutdown();
                    break;
            }
        }
    }

    public abstract SurfaceTexture getVideoTexture();

    interface RenderCallback {
        void onInit();

        void onSizeChanged(int width, int height);

        void onDraw();

        void onDestroy();
    }

}
