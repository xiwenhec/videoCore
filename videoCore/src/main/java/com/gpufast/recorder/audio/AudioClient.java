package com.gpufast.recorder.audio;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.audio.encoder.AudioEncoder;

import java.lang.ref.WeakReference;

public class AudioClient implements AudioCollector.OnAudioFrameCallback {

    //音频编码线程
    private AudioEncoderThread mEncoderThread;

    private EncoderHandler mEncoderHandler;

    //音频采集器
    private AudioCollector mAudioCollector;

    //音频预处理器
    private AudioProcessor mAudioPreprocessor;


    public AudioClient(AudioEncoder encoder,
                       AudioSetting settings,
                       AudioEncoder.AudioEncoderCallback callback) {
        if (encoder == null || settings == null) return;

        mAudioCollector = new AudioCollector();
        mAudioCollector.init(settings, this);
        mEncoderThread = new AudioEncoderThread(encoder, settings, callback);
    }


    public void start() {
        //启动音频编码线程
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
        //启动音频采集器
        mAudioCollector.start();
        mEncoderHandler = mEncoderThread.getHandler();
    }

    /**
     * 停止编码线程
     */
    public void stop() {
        if (mAudioCollector != null) {
            mAudioCollector.stop();
        }
        if (mEncoderHandler != null && mEncoderThread.mReady) {
            mEncoderHandler.stop();
            mEncoderThread.waitUntilStop();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        stop();
    }

    /**
     * 设置音频预处理器
     *
     * @param preprocessor 处理器对象
     */
    public void setAudioPreprocessor(AudioProcessor preprocessor) {
        mAudioPreprocessor = preprocessor;
    }

    @Override
    public void onReceiveAudioFrame(AudioFrame frame) {
        AudioFrame newFrame = null;
        if (mAudioPreprocessor != null) {
            newFrame = mAudioPreprocessor.onReceiveAudioFrame(frame);
        }
        if (newFrame != null) {
            frame = newFrame;
        }
        if (mEncoderThread.mReady) {
            mEncoderThread.getHandler().sendAudioFrame(frame);
        }
    }


    private static class AudioEncoderThread extends Thread {
        private static final String TAG = AudioEncoderThread.class.getSimpleName();
        private final Object mStartLock = new Object();
        private volatile boolean mReady = false;
        private EncoderHandler mEncoderHandler;


        private AudioEncoder mAudioEncoder;
        private AudioSetting mSettings;

        AudioEncoder.AudioEncoderCallback mCallback;

        AudioEncoderThread(AudioEncoder encoder, AudioSetting settings,
                           AudioEncoder.AudioEncoderCallback callback) {
            super("audio_Encoder_Thread");
            mAudioEncoder = encoder;
            mSettings = settings;
            mCallback = callback;
        }

        @Override
        public void run() {
            Looper.prepare();
            mEncoderHandler = new EncoderHandler(this);
            initEncoder();
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();
            }
            Looper.loop();
            release();
            synchronized (mStartLock) {
                mReady = false;
                mStartLock.notify();
            }
            ELog.i(TAG, "audio encoder thread quit.");
        }

        void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException e) { /* not expected */ }
                }
            }
        }

        void waitUntilStop() {
            synchronized (mStartLock) {
                while (mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException e) { /* not expected */ }
                }
            }
        }

        private void initEncoder() {
            if (mAudioEncoder != null) {
                mAudioEncoder.init(mSettings, mCallback);
            }
        }

        void sendAudioFrame(AudioFrame frame) {
            if (mAudioEncoder != null && mReady) {
                mAudioEncoder.encode(frame);
            }
        }

        EncoderHandler getHandler() {
            return mEncoderHandler;
        }

        private void shutdown() {
            Looper.myLooper().quit();
        }

        private void release() {
            if (mAudioEncoder != null) {
                mAudioEncoder.release();
                mAudioEncoder = null;
            }
        }
    }


    private static class EncoderHandler extends Handler {

        private static final String TAG = EncoderHandler.class.getSimpleName();

        private static final int MSG_FRAME_AVAILABLE = 0x001;
        private static final int MSG_STOP = 0x002;

        private WeakReference<AudioClient.AudioEncoderThread> mWeakEncoderThread;

        EncoderHandler(AudioEncoderThread thread) {
            mWeakEncoderThread = new WeakReference<>(thread);
        }


        void sendAudioFrame(AudioFrame frame) {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE, frame));
        }

        private void stop() {
            sendMessage(obtainMessage(MSG_STOP));
        }

        @Override
        public void handleMessage(Message msg) {
            AudioEncoderThread encoderThread = mWeakEncoderThread.get();
            if (encoderThread == null) {
                ELog.e(TAG, "mWeakEncoderThread.get() == null");
                return;
            }
            switch (msg.what) {
                case MSG_FRAME_AVAILABLE:
                    encoderThread.sendAudioFrame((AudioFrame) msg.obj);
                    break;
                case MSG_STOP:
                    encoderThread.shutdown();
                    break;
            }
        }
    }
}
