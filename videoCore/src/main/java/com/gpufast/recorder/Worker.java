package com.gpufast.recorder;


import android.opengl.EGLContext;
import android.os.Looper;
import android.os.Message;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.audio.AudioProcessor;

import java.lang.ref.WeakReference;

class Worker extends BaseWorker {

    private IRecorder recorder;
    private WorkerHandler mWorkHandler;

    Worker() {
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();
        workerThread.waitUntilReady();
        recorder = workerThread.getRecorder();
        mWorkHandler = workerThread.getHandler();
    }

    @Override
    public void setParams(RecordParams params) {
        if (mWorkHandler != null) {
            mWorkHandler.setParams(params);
        }
    }

    @Override
    public void setShareContext(EGLContext shareContext) {
        //不用转发到同一个线程
        if (mWorkHandler != null) {
            mWorkHandler.setShareContext(shareContext);
        }
    }

    @Override
    public void sendVideoFrame(int textureId, int srcWidth, int srcHeight) {
        //不用转发到同一个线程
        if (recorder != null) {
            recorder.sendVideoFrame(textureId, srcWidth, srcHeight);
        }
    }


    @Override
    public void startRecorder() {
        if (mWorkHandler != null) {
            mWorkHandler.startRecorder();
        }
    }

    @Override
    public void stopRecorder() {
        if (mWorkHandler != null) {
            mWorkHandler.stopRecorder();
        }
    }

    @Override
    public boolean isRecording() {
        return recorder != null && recorder.isRecording();
    }

    @Override
    public void jointVideo() {
        if (mWorkHandler != null) {
            mWorkHandler.jointVideo();
        }
    }

    @Override
    public void setRecordListener(RecordListener listener) {
        //不需要做线程同步
        if (recorder != null) {
            recorder.setRecordListener(listener);
        }
    }

    @Override
    public void setAudioProcessor(AudioProcessor processor) {
        if (recorder != null) {
            recorder.setAudioProcessor(processor);
        }
    }

    @Override
    public void release() {
        if (mWorkHandler != null) {
            mWorkHandler.release();
        }
        mWorkHandler = null;
        recorder = null;
    }

    private static class WorkerThread extends BaseWorkerThread {
        private static final String TAG = WorkerThread.class.getSimpleName();
        private final Object mStartLock = new Object();
        private boolean mReady = false;

        private WorkerHandler mHandler;
        private IRecorder mRecorder;

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new WorkerHandler(this);
            mRecorder = new EffectRecorder();
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();
            }
            Looper.loop();
        }

        @Override
        public void setParams(RecordParams params) {
            if (mRecorder != null) {
                mRecorder.setParams(params);
            }
        }

        @Override
        public void setShareContext(EGLContext shareContext) {
            if(mRecorder != null){
                mRecorder.setShareContext(shareContext);
            }
        }

        @Override
        public void startRecorder() {
            if (mRecorder != null) {
                mRecorder.startRecorder();
            }

        }

        @Override
        public void stopRecorder() {
            if (mRecorder != null) {
                mRecorder.stopRecorder();
            }

        }

        @Override
        public boolean isRecording() {
            if (mRecorder == null) {
                return false;
            }
            return mRecorder.isRecording();
        }

        @Override
        public void jointVideo() {
            if (mRecorder != null) {
                mRecorder.jointVideo();
            }
        }

        @Override
        public void release() {
            stopRecorder();
            Looper.myLooper().quit();
        }

        void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException e) {
                        ELog.i(TAG, "An error occurred " +
                                "on the worker thread:" + e.getLocalizedMessage());
                    }
                }
            }
        }

        WorkerHandler getHandler() {
            return mHandler;
        }

        public IRecorder getRecorder() {
            return mRecorder;
        }
    }


    private static class WorkerHandler extends BaseWorkerHandler {

        private static final int MSG_SET_PARAMS = 1;
        private static final int MSG_SET_EGL_CONTEXT = 2;
        private static final int MSG_START_RECORDER = 3;
        private static final int MSG_STOP_RECORDER = 4;
        private static final int MSG_JOINT_VIDEO = 5;
        private static final int MSG_RELEASE = 6;

        private WeakReference<WorkerThread> mWeakWorkThread;

        WorkerHandler(WorkerThread thread) {
            mWeakWorkThread = new WeakReference<>(thread);
        }

        @Override
        public void setParams(RecordParams params) {
            sendMessage(obtainMessage(MSG_SET_PARAMS,params));
        }

        @Override
        public void setShareContext(EGLContext shareContext) {
            sendMessage(obtainMessage(MSG_SET_EGL_CONTEXT,shareContext));
        }

        @Override
        public void startRecorder() {
            sendMessage(obtainMessage(MSG_START_RECORDER));
        }

        @Override
        public void stopRecorder() {
            sendMessage(obtainMessage(MSG_STOP_RECORDER));
        }

        @Override
        public void jointVideo() {
            sendMessage(obtainMessage(MSG_JOINT_VIDEO));
        }

        @Override
        public void release() {
            sendMessage(obtainMessage(MSG_RELEASE));
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerThread workerThread = mWeakWorkThread.get();
            if (workerThread == null) return;
            switch (msg.what) {
                case MSG_SET_PARAMS:
                    workerThread.setParams((RecordParams) msg.obj);
                    break;
                case MSG_SET_EGL_CONTEXT:
                    workerThread.setShareContext((EGLContext) msg.obj);
                    break;
                case MSG_START_RECORDER:
                    workerThread.startRecorder();
                    break;
                case MSG_STOP_RECORDER:
                    workerThread.stopRecorder();
                    break;
                case MSG_JOINT_VIDEO:
                    workerThread.jointVideo();
                    break;
                case MSG_RELEASE:
                    workerThread.release();
                    break;
            }
        }
    }
}
