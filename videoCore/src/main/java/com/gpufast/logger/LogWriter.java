//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gpufast.logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;

class LogWriter {
    private static final String LOG_WRITE_THREAD_NAME = "com.gpufast.loggerWriter";
    private static final String LOG_FILENAME = "effect_sdk.log";
    private static LogWriter instance;
    private WriterThread mWriterThread;

    private LogWriter() {
        mWriterThread = new WriterThread(LOG_WRITE_THREAD_NAME);
        mWriterThread.start();
        mWriterThread.waitUntilReady();
        mWriterThread.getHandler().tryOpenLogFile();
    }

    public static LogWriter getInstance() {
        if (instance == null) {
            synchronized (LogWriter.class) {
                if (instance == null) {
                    instance = new LogWriter();
                }
            }
        }
        return instance;
    }

    void writer(String log) {
        WriterHandler handler = mWriterThread.getHandler();
        if (handler != null) {
            handler.writerMsg(log);
        }
    }

    void shutdown() {
        WriterHandler handler = mWriterThread.getHandler();
        if (handler != null) {
            handler.shutdown();
        }
    }

    private static class WriterThread extends Thread {
        private final Object mStartLock = new Object();
        private boolean mReady = false;
        private WriterHandler mHandler;
        private FileWriter fileWriter;

        WriterThread(String name) {
            super(name);
        }

        WriterHandler getHandler() {
            return mHandler;
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new WriterHandler(this);
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();
            }
            Looper.loop();
            release();
            mReady = false;
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

        private void shutdown() {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        }

        private void openLogFile() {
            try {
                File logDir = new File(LogEntity.getInstance().getLogDir());
                boolean ret = false;
                if (!logDir.exists()) {
                    ret = logDir.mkdirs();
                }
                if (ret) {
                    File logFile = new File(logDir, LOG_FILENAME);
                    if (logFile.exists()) {
                        if (logFile.length() > 1024 * 1024 * 20) {
                            ret = logFile.delete();
                        }
                    }
                    if (!ret) {
                        shutdown();
                        return;
                    }
                    fileWriter = new FileWriter(logFile, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
                fileWriter = null;
            }
        }

        private void write(String log) {
            try {
                if (fileWriter != null) {
                    fileWriter.write(log + "\n");
                    fileWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void release() {
            mHandler = null;
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class WriterHandler extends Handler {

        private static final int ON_MSG_OPEN_FILE = 0;
        private static final int ON_MSG_AVAILABLE = 1;
        private static final int ON_MSG_SHUTDOWN = 2;

        private WeakReference<WriterThread> mWeakRenderThread;

        WriterHandler(WriterThread thread) {
            mWeakRenderThread = new WeakReference<>(thread);
        }

        void tryOpenLogFile() {
            sendMessage(obtainMessage(ON_MSG_OPEN_FILE));
        }

        void writerMsg(String msg) {
            sendMessage(obtainMessage(ON_MSG_AVAILABLE, 0, 0, msg));
        }

        void shutdown() {
            sendMessage(obtainMessage(ON_MSG_SHUTDOWN));
        }


        @Override
        public void handleMessage(Message msg) {
            WriterThread writerThread = mWeakRenderThread.get();
            if (writerThread == null) {
                return;
            }
            int what = msg.what;
            switch (what) {
                case ON_MSG_OPEN_FILE:
                    writerThread.openLogFile();
                    break;

                case ON_MSG_AVAILABLE:
                    writerThread.write((String) msg.obj);
                    break;

                case ON_MSG_SHUTDOWN:
                    writerThread.shutdown();
                    break;
            }
        }
    }

}
