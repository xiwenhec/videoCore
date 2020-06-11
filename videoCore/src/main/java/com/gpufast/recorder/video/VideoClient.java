package com.gpufast.recorder.video;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.PresentationTime;

import java.lang.ref.WeakReference;

/**
 * 转发前端传递的图像数据，交给编码前局处理部分
 * 然后送给编码器进行编码
 */
public class VideoClient {
    private static final String TAG = "VideoClient";
    private VideoEncoderThread mEncoderThread;
    private PresentationTime pTime;

    public VideoClient(VideoEncoder encoder,
                       VideoEncoder.Settings settings,
                       VideoEncoder.VideoEncoderCallback callback) {

        mEncoderThread = new VideoEncoderThread(encoder, settings, callback);
        pTime = new PresentationTime(settings.maxFrameRate);
    }


    public void start() {
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
        pTime.start();
    }


    public void sendVideoFrame(int textureId, int srcWidth, int srcHeight) {
        if (mEncoderThread != null && mEncoderThread.isReady()) {
            pTime.record();
            VideoFrame videoFrame = new VideoFrame(
                    new TextureBufferImpl(textureId, srcWidth,
                            srcHeight, VideoFrame.TextureBuffer.TextureType.RGB),
                    0, pTime.presentationTimeNs);
            mEncoderThread.getHandler().sendVideoFrame(videoFrame);
        }
    }

    public void stop() {
        if (mEncoderThread.isReady()) {
            mEncoderThread.getHandler().sendToStop();
            mEncoderThread.waitUntilStop();
            ELog.i(TAG, "video client has stop");
        }
    }

    private static class VideoEncoderThread extends Thread {
        private static final String TAG = VideoEncoderThread.class.getSimpleName();
        private final Object mLock = new Object();
        private boolean mReady = false;
        private EncoderHandler mEncoderHandler;
        private VideoEncoder mVideoEncoder;
        private VideoEncoder.Settings mSettings;
        VideoEncoder.VideoEncoderCallback mCallback;

        VideoEncoderThread(VideoEncoder encoder, VideoEncoder.Settings settings,
                           VideoEncoder.VideoEncoderCallback callback) {
            mVideoEncoder = encoder;
            mSettings = settings;
            mCallback = callback;
        }

        boolean isReady() {
            return mReady;
        }

        EncoderHandler getHandler() {
            return mEncoderHandler;
        }

        @Override
        public void run() {
            Looper.prepare();
            mEncoderHandler = new EncoderHandler(this);
            initEncoder();
            synchronized (mLock) {
                mReady = true;
                mLock.notify();
            }
            Looper.loop();
            deInitEncoder();
            synchronized (mLock) {
                mReady = false;
                mLock.notify();
            }
            ELog.i(TAG, "video encoder thread quit.");
        }

        void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        void waitUntilStop() {
            synchronized (mLock) {
                while (mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        private void initEncoder() {
            if (mVideoEncoder != null) {
                mVideoEncoder.init(mSettings, mCallback);
            }
        }

        void sendVideoFrame(VideoFrame frame) {
            if (mVideoEncoder != null && mReady) {
                mVideoEncoder.encode(frame);
            }
        }

        private void shutdown() {
            Looper.myLooper().quit();
        }


        private void deInitEncoder() {
            if (mVideoEncoder != null) {
                mVideoEncoder.deInit();
                mVideoEncoder = null;
            }
        }
    }


    private static class EncoderHandler extends Handler {
        private static final String TAG = EncoderHandler.class.getSimpleName();
        private static final int ON_FRAME_AVAILABLE = 0x001;
        private static final int ON_STOP = 0x002;

        private WeakReference<VideoClient.VideoEncoderThread> mWeakEncoderThread;

        EncoderHandler(VideoClient.VideoEncoderThread thread) {
            mWeakEncoderThread = new WeakReference<>(thread);
        }


        public void sendVideoFrame(VideoFrame frame) {
            sendMessage(obtainMessage(ON_FRAME_AVAILABLE, frame));
        }

        private void sendToStop() {
            sendMessage(obtainMessage(ON_STOP));
        }

        @Override
        public void handleMessage(Message msg) {

            VideoEncoderThread encoderThread = mWeakEncoderThread.get();
            if (encoderThread == null) {
                ELog.e(TAG, "mWeakEncoderThread.get() == null");
                return;
            }
            switch (msg.what) {
                case ON_FRAME_AVAILABLE:
                    encoderThread.sendVideoFrame((VideoFrame) msg.obj);
                    break;
                case ON_STOP:
                    encoderThread.shutdown();
                    break;
            }
        }
    }


    private static class TextureBufferImpl implements VideoFrame.TextureBuffer {

        private int width;
        private int height;
        private int textureId;
        private TextureType type;

        public TextureBufferImpl(int textureId, int width, int height, TextureType type) {
            this.width = width;
            this.height = height;
            this.textureId = textureId;
            this.type = type;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public VideoFrame.I420Buffer toI420() {
            return null;
        }

        @Override
        public void release() {

        }

        @Override
        public VideoFrame.Buffer cropAndScale(int cropX, int cropY, int cropWidth, int cropHeight, int scaleWidth, int scaleHeight) {
            return null;
        }

        @Override
        public TextureType getType() {
            return type;
        }

        @Override
        public int getTextureId() {
            return textureId;
        }

        @Override
        public Matrix getTransformMatrix() {
            return null;
        }

    }

}
