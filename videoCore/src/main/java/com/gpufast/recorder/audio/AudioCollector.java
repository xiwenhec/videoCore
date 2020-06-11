package com.gpufast.recorder.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.gpufast.logger.ELog;

import java.nio.ByteBuffer;

public class AudioCollector {
    private static final String TAG = "AudioCollector";


    private AudioRecord mAudioRecord;

    private AudioCollectThread collectThread;

    private OnAudioFrameCallback callback;

    private int minBufferSize;

    public void init(AudioSetting settings, OnAudioFrameCallback callback) {
        minBufferSize = settings.getInputBufferSize();
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            ELog.e(TAG, "AudioRecord.getMinBufferSize failed: " + minBufferSize);
        }
        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                settings.getSampleRate(),
                settings.getChannelConfig(),
                settings.getAudioFormat(), minBufferSize);
        this.callback = callback;
    }

    public void start() {
        collectThread = new AudioCollectThread(callback);
        collectThread.start();
        //检测等待录音器是否启动录制
        collectThread.waitUntilReady();
    }


    void stop() {
        if (collectThread != null) {
            collectThread.stopThread();
            collectThread = null;
            ELog.i(TAG, "audio collector has stop");
        }
    }

    /**
     * 音频采集线程
     */
    class AudioCollectThread extends Thread {
        private volatile boolean keepAlive = true;
        private final Object mStartLock = new Object();
        private boolean mReady = false;
        private OnAudioFrameCallback callback;

        AudioCollectThread(OnAudioFrameCallback callback) {
            super("AudioCollectThread");
            this.callback = callback;
        }

        @Override
        public void run() {
            //没有数据接收的地方，采集无意义
            if (callback == null || mAudioRecord == null) return;
            long start = System.currentTimeMillis();
            //等待检测录音器启动录制是否成功
            mAudioRecord.startRecording();
            while (keepAlive) {
                int state = mAudioRecord.getRecordingState();
                if (state == AudioRecord.RECORDSTATE_STOPPED) {
                    if (System.currentTimeMillis() - start < 500) {
                        yield();
                    } else {
                        //如果500毫秒内录音器未初始化成功，则放弃录音
                        synchronized (mStartLock) {
                            mReady = true;
                            mStartLock.notify();  //释放等待线程
                        }
                        ELog.e(TAG, "audio AudioCollectThread start failed.");
                        return;
                    }
                } else {
                    break;
                }
            }
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();
            }

            //------------------------开始读取麦克风数据------------------
            ByteBuffer buffer = ByteBuffer.allocateDirect(minBufferSize);
            start = System.nanoTime();
            while (keepAlive) {
                buffer.clear();
                int num = mAudioRecord.read(buffer, buffer.limit());
                if (num > 0) {
                    callback.onReceiveAudioFrame(new AudioFrame(buffer, num, (System.nanoTime() - start) / 1000));
                }
            }

            //----------------开始停止录音---------------------------------
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
            synchronized (mStartLock) {
                mReady = false;
                mStartLock.notify();
            }
        }


        void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        void stopThread() {
            keepAlive = false;
            //防止录音失败，Stop处于一直等待状态
            if (mReady) {
                waitUntilStop();
            }
        }

        void waitUntilStop() {
            synchronized (mStartLock) {
                while (mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }
    }

    public interface OnAudioFrameCallback {
        void onReceiveAudioFrame(AudioFrame frame);
    }


}
