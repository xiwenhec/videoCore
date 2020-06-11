/*
 *  Copyright 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.gpufast.recorder.video.encoder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.view.Surface;

import com.gpufast.gles.EglCore;
import com.gpufast.logger.ELog;
import com.gpufast.recorder.hardware.MediaCodecWrapper;
import com.gpufast.recorder.hardware.MediaCodecWrapperFactory;
import com.gpufast.recorder.video.EncodedImage;
import com.gpufast.recorder.video.VideoEncoder;
import com.gpufast.recorder.video.VideoFrame;
import com.gpufast.recorder.video.btadjuster.BitrateAdjuster;
import com.gpufast.recorder.video.renderer.GlRectDrawer;
import com.gpufast.recorder.video.renderer.VideoFrameDrawer;
import com.gpufast.utils.ThreadUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Android hardware video encoder.
 */
@TargetApi(19)
class HwVideoEncoder implements VideoEncoder {

    private static final String TAG = "HwVideoEncoder";

    /**
     * 码率控制模式---value api21之前没有这个参数{@link android.media.MediaCodecInfo.EncoderCapabilities#BITRATE_MODE_CBR}
     */
    private static final int VIDEO_ControlRateConstant = 2;
    /**
     * 码率控制模式--key，在api21之前没有这个常量 {@link MediaFormat#KEY_BITRATE_MODE}
     */
    private static final String KEY_BITRATE_MODE = "bitrate-mode";

    private static final int PROFILE_BASELINE = 0x01;
    private static final int PROFILE_MAIN = 0x02;
    private static final int PROFILE_HEIGHT = 0x08;
    private static final int VIDEO_AVC_LEVEL_3 = 0x100;

    private static final int MAX_ENCODER_Q_SIZE = 2;

    private static final int MEDIA_CODEC_RELEASE_TIMEOUT_MS = 5000;
    private static final int DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = 100000;

    private final MediaCodecWrapperFactory mediaCodecWrapperFactory;
    //编码器名称
    private final String codecName;
    //编码器类型H264
    private final VideoCodecType codecType;
    //surface的颜色格式
    private final Integer surfaceColorFormat;

    private final Map<String, String> params;
    //关键帧间隔时间(秒)
    private final int keyFrameIntervalSec;

    private final BitrateAdjuster bitrateAdjuster;
    private final EGLContext sharedContext;

    // Drawer used to draw input textures onto the codec's input surface.
    private final GlRectDrawer textureDrawer = new GlRectDrawer();
    private final VideoFrameDrawer videoFrameDrawer = new VideoFrameDrawer();

    //所有不能通过MediaCodec传递的编码信息，都将放到这里
    private final BlockingDeque<EncodedImage.Builder> outputBuilders = new LinkedBlockingDeque<>();

    private final ThreadUtils.ThreadChecker encodeThreadChecker = new ThreadUtils.ThreadChecker();
    private final ThreadUtils.ThreadChecker outputThreadChecker = new ThreadUtils.ThreadChecker();

    //初始化后不能改变，直到释放
    private VideoEncoderCallback encoderCallback;

    private MediaCodecWrapper codec;
    private Thread outputThread;
    private EglCore mEglCore;
    private Surface textureInputSurface;

    private int width;
    private int height;

    // --- Only accessed on the output thread.
    //h264配置buffer
    private ByteBuffer configBuffer;
    private int adjustedBitrate;
    //编码器是否正在运行，Volatile，保证编码停止的时候能准确及时观察到值
    private volatile boolean running;
    // Any exception thrown during shutdown.  The output thread releases the MediaCodec and uses this
    // value to send exceptions thrown during deInit back to the encoder thread.
    private volatile Exception shutdownException;
    //将要编码的序列号，用来检测丢帧
    private int frameIndex = 0;

    /**
     * Creates a new HwVideoEncoder with the given codecName, codecType, colorFormat, key frame intervals, and
     * bitrateAdjuster.
     *
     * @param codecName           硬编码器名字
     * @param codecType           编码器类型（VP8,VP9,H264)
     * @param surfaceColorFormat  color format for surface mode or null if not available
     * @param keyFrameIntervalSec 两个关键帧之间的间隔单位秒; 用于初始化编码器
     * @param bitrateAdjuster     纠正编码器输出非期望码率的算法
     * @throws IllegalArgumentException if colorFormat is unsupported
     */
    HwVideoEncoder(MediaCodecWrapperFactory mediaCodecWrapperFactory, String codecName,
                   VideoCodecType codecType, Integer surfaceColorFormat,
                   Map<String, String> params, int keyFrameIntervalSec,
                   BitrateAdjuster bitrateAdjuster, EGLContext sharedContext) {
        this.mediaCodecWrapperFactory = mediaCodecWrapperFactory;
        this.codecName = codecName;
        this.codecType = codecType;
        this.surfaceColorFormat = surfaceColorFormat;
        this.params = params;
        this.keyFrameIntervalSec = keyFrameIntervalSec;
        this.bitrateAdjuster = bitrateAdjuster;
        this.sharedContext = sharedContext;
        // 构造函数可以执行在其他线程中
        encodeThreadChecker.detachThread();
    }

    @Override
    public VideoCodecStatus init(Settings settings, VideoEncoderCallback callback) {
        encodeThreadChecker.checkIsOnValidThread();
        this.encoderCallback = callback;
        this.width = settings.width;
        this.height = settings.height;

        if (settings.startBitrate != 0 && settings.maxFrameRate != 0) {
            bitrateAdjuster.setTargets(settings.startBitrate * 1000, settings.maxFrameRate);
        }

        adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();

        ELog.i(TAG,
                "init: " + width + " x " + height + ". @ " + settings.startBitrate
                        + "kbps. Fps: " + settings.maxFrameRate);

        return initEncodeInternal();
    }



    private VideoCodecStatus initEncodeInternal() {

        encodeThreadChecker.checkIsOnValidThread();
        try {
            codec = mediaCodecWrapperFactory.createByCodecName(codecName);
        } catch (IOException | IllegalArgumentException e) {
            ELog.e(TAG, "Cannot create media encoder " + codecName);
            return VideoCodecStatus.FALLBACK_SOFTWARE;
        }


        final int colorFormat = surfaceColorFormat;

        try {
            MediaFormat format = MediaFormat.createVideoFormat(codecType.mimeType(), width, height);
            //设置码率
            ELog.i(HwVideoEncoder.class, "rate:" + adjustedBitrate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, adjustedBitrate);
            // //设置码率控制模式
            // format.setInteger(KEY_BITRATE_MODE, VIDEO_ControlRateConstant);
            //配置颜色格式
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            //配置帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, bitrateAdjuster.getCodecConfigFrameRate());
            //配置关键帧间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameIntervalSec);

            //配置H264 profile 和level
            if (codecType == VideoCodecType.H264) {
                String profile = params.get(VideoCodecInfo.H264_PROFILE);
                ELog.d(TAG, "video profile =" + profile);
                if (profile == null)
                    return VideoCodecStatus.ERROR;
                switch (profile) {
                    case VideoCodecInfo.VALUE_BASE_LINE:
                        format.setInteger(VideoCodecInfo.H264_PROFILE, PROFILE_BASELINE);
                        format.setInteger("level", VIDEO_AVC_LEVEL_3);
                        break;
                    case VideoCodecInfo.VALUE_main:
                        format.setInteger(VideoCodecInfo.H264_PROFILE, PROFILE_MAIN);
                        format.setInteger("level", VIDEO_AVC_LEVEL_3);
                        break;
                    case VideoCodecInfo.VALUE_height:
                        format.setInteger(VideoCodecInfo.H264_PROFILE, PROFILE_HEIGHT);
                        format.setInteger("level", VIDEO_AVC_LEVEL_3);
                        break;
                    default:
                        ELog.w(TAG, "Unknown profile: " + profile);
                }
            }
            ELog.i(TAG, " video Format: " + format);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEglCore = EglCore.create(sharedContext, EglCore.CONFIG_RECORDABLE);
            textureInputSurface = codec.createInputSurface();
            mEglCore.createSurface(textureInputSurface);
            mEglCore.makeCurrent();
            ELog.i(TAG, "name:" + codecName + " codec.start(): ");
            frameIndex = 0;
            codec.start();

        } catch (IllegalStateException e) {
            ELog.e(TAG, "initEncodeInternal failed:" + e.getLocalizedMessage());
            deInit();
            return VideoCodecStatus.FALLBACK_SOFTWARE;
        }
        running = true;
        outputThreadChecker.detachThread();
        outputThread = createOutputThread();
        outputThread.start();
        return VideoCodecStatus.OK;
    }

    @Override
    public VideoCodecStatus encode(VideoFrame videoFrame) {
        encodeThreadChecker.checkIsOnValidThread();
        if (codec == null) {
            return VideoCodecStatus.UNINITIALIZED;
        }

        //如果输入分辨率发生变化，则使用新分辨率重新启动编解码器
        final int frameWidth = videoFrame.getBuffer().getWidth();
        final int frameHeight = videoFrame.getBuffer().getHeight();

        //编码期间宽高发生改变，重置编码器
        if (frameWidth != width || frameHeight != height) {
            VideoCodecStatus status = resetCodec(frameWidth, frameHeight);
            if (status != VideoCodecStatus.OK) {
                return status;
            }
        }

        frameIndex++;
        if (outputBuilders.size() > MAX_ENCODER_Q_SIZE) {
            //编码器中的有太多帧数据，需要丢掉该帧
            ELog.e(TAG, "Dropped frame, encoder queue full");
            return VideoCodecStatus.NO_OUTPUT;
        }

        EncodedImage.Builder builder = EncodedImage.builder()
                .setCaptureTimeNs(videoFrame.getTimestampNs())
                .setCompleteFrame(true)
                .setEncodedWidth(videoFrame.getBuffer().getWidth())
                .setEncodedHeight(videoFrame.getBuffer().getHeight())
                .setFrameIndex(frameIndex)
                .setRotation(videoFrame.getRotation());

        outputBuilders.offer(builder);

        final VideoCodecStatus returnValue = encodeTextureBuffer(videoFrame);

        if (returnValue != VideoCodecStatus.OK) {
            outputBuilders.pollLast();
        }

        return returnValue;
    }

    private VideoCodecStatus encodeTextureBuffer(VideoFrame videoFrame) {
        encodeThreadChecker.checkIsOnValidThread();
        try {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // 没有必要去释放这个frame，因为它没有buffer数据.
            VideoFrame deRotatedFrame = new VideoFrame(videoFrame.getBuffer(), 0, videoFrame.getTimestampNs());
            videoFrameDrawer.drawFrame(deRotatedFrame, textureDrawer, null);
            mEglCore.swapBuffers(videoFrame.getTimestampNs());
        } catch (RuntimeException e) {
            ELog.e(TAG, "encodeTexture failed:" + e.getLocalizedMessage());
            return VideoCodecStatus.ERROR;
        }
        return VideoCodecStatus.OK;
    }

    @Override
    public String getImplementationName() {
        return "HWEncoder";
    }

    /**
     * 重置编码器
     *
     * @param newWidth  newWidth
     * @param newHeight newHeight
     * @return VideoCodecStatus
     */
    private VideoCodecStatus resetCodec(int newWidth, int newHeight) {
        encodeThreadChecker.checkIsOnValidThread();
        VideoCodecStatus status = deInit();
        if (status != VideoCodecStatus.OK) {
            return status;
        }
        width = newWidth;
        height = newHeight;
        return initEncodeInternal();
    }

    private Thread createOutputThread() {
        return new Thread() {
            @Override
            public void run() {
                ELog.i(TAG, "video output thread start.");
                while (running) {
                    deliverEncodedImage();
                }
                releaseCodecOnOutputThread();
            }
        };
    }

    /**
     * 开启线程从Encoder里不断的读取编码后的h264数据
     */
    private void deliverEncodedImage() {
        outputThreadChecker.checkIsOnValidThread();
        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int index = codec.dequeueOutputBuffer(info, DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US);
            if (index >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                outputBuffer.position(info.offset);
                outputBuffer.limit(info.offset + info.size);

                //表示该buffer是一个编码配置信息buffer
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    ELog.i(TAG, "Config frame generated. Offset: " + info.offset + ". Size: " + info.size);
                    configBuffer = ByteBuffer.allocateDirect(info.size);
                    configBuffer.put(outputBuffer);
                } else {
                    //判断是否是关键帧
                    final boolean isKeyFrame = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
                    //这个buffer必须是一个新的对象
                    final ByteBuffer frameBuffer;
                    if (isKeyFrame && codecType == VideoCodecType.H264) {
                        ELog.d(TAG,
                                "Prepending config frame of size " + configBuffer.capacity()
                                        + " to output buffer with offset " + info.offset + ", size " + info.size);
                        // For H.264 key frame prepend SPS and PPS NALs at the start.
                        //分配buffer的size= keyFrame+config
                        frameBuffer = ByteBuffer.allocateDirect(info.size + configBuffer.capacity());
                        configBuffer.rewind();
                        frameBuffer.put(configBuffer);
                        frameBuffer.put(outputBuffer);
                        frameBuffer.rewind();
                    } else {
                        frameBuffer = outputBuffer.slice();
                    }

                    final EncodedImage.FrameType frameType = isKeyFrame
                            ? EncodedImage.FrameType.VideoFrameKey
                            : EncodedImage.FrameType.VideoFrameDelta;
                    EncodedImage.Builder builder = outputBuilders.poll();
                    if (builder == null) return;
                    builder.setBuffer(frameBuffer)
                            .setBufferInfo(info)
                            .setFrameType(frameType);
                    if (encoderCallback != null) {
                        encoderCallback.onEncodedFrame(builder.createEncodedImage());
                    }
                }
                codec.releaseOutputBuffer(index, info.presentationTimeUs);
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (encoderCallback != null) {
                    encoderCallback.onUpdateVideoMediaFormat(codec.getOutputFormat());
                }
            }
        } catch (Exception e) {
            ELog.e(TAG, "deliver Output failed:" + e.getLocalizedMessage());
        }
    }

    private void releaseCodecOnOutputThread() {
        outputThreadChecker.checkIsOnValidThread();
        ELog.d(TAG, "Releasing MediaCodec on output thread");
        try {
            codec.stop();
        } catch (Exception e) {
            ELog.e(TAG, "Media encoder stop failed:" + e.getLocalizedMessage());
        }
        try {
            codec.release();
        } catch (Exception e) {
            ELog.e(TAG, "Media encoder deInit failed:" + e.getLocalizedMessage());
            shutdownException = e;
        }
        configBuffer = null;
        ELog.d(TAG, "Release on output thread done");
        if (encoderCallback != null) {
            encoderCallback.onVideoEncoderStop();
        }
    }

//    private VideoCodecStatus updateBitrate() {
//        outputThreadChecker.checkIsOnValidThread();
//        adjustedBitrate = bitrateAdjuster.getAdjustedBitrateBps();
//        try {
//            Bundle params = new Bundle();
//            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, adjustedBitrate);
//            codec.setParameters(params);
//            return VideoCodecStatus.OK;
//        } catch (IllegalStateException e) {
//            ELog.e(TAG, "updateBitrate failed:" + e.getLocalizedMessage());
//            return VideoCodecStatus.ERROR;
//        }
//    }

    @Override
    public VideoCodecStatus deInit() {
        encodeThreadChecker.checkIsOnValidThread();
        final VideoCodecStatus returnValue;
        if (outputThread == null) {
            returnValue = VideoCodecStatus.OK;
        } else {
            // The outputThread actually stops and releases the codec once running is false.
            running = false;
            if (!ThreadUtils.joinUninterruptibly(outputThread, MEDIA_CODEC_RELEASE_TIMEOUT_MS)) {
                ELog.e(TAG, "Media encoder deInit timeout");
                returnValue = VideoCodecStatus.TIMEOUT;
            } else if (shutdownException != null) {
                // Log the exception and turn it into an error.
                ELog.e(TAG, "Media encoder deInit exception:" + shutdownException.getLocalizedMessage());
                returnValue = VideoCodecStatus.ERROR;
            } else {
                returnValue = VideoCodecStatus.OK;
            }
        }
        textureDrawer.release();
        videoFrameDrawer.release();
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (textureInputSurface != null) {
            textureInputSurface.release();
            textureInputSurface = null;
        }
        outputBuilders.clear();
        codec = null;
        outputThread = null;
        // Allow changing thread after deInit.
        encodeThreadChecker.detachThread();

        return returnValue;
    }

}
