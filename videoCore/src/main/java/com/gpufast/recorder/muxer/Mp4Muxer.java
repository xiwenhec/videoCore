package com.gpufast.recorder.muxer;

import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.gpufast.logger.ELog;
import com.gpufast.recorder.audio.EncodedAudio;
import com.gpufast.recorder.video.EncodedImage;

import java.io.IOException;

/**
 * 视频合成接口
 */
public class Mp4Muxer extends IMediaMuxer {
    private static final String TAG = Mp4Muxer.class.getSimpleName();

    private MediaMuxer mMediaMuxer;
    private int audioTrackIndex = -1;
    private int videoTrackIndex = -1;
    private volatile int trackCount = 0;
    private boolean muxerStarted = false;
    private boolean videoTrackReady = false;
    private boolean audioTrackReady = false;

    private boolean muteMic;


    Mp4Muxer(Setting setting) {
        if (setting == null)
            throw new IllegalArgumentException("setting is null object");
        try {
            mMediaMuxer = new MediaMuxer(setting.savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initMuxer(setting);
        } catch (IOException e) {
            ELog.e(TAG, "Init IMediaMuxer:" + e.getMessage());
        }
    }

    private void initMuxer(Setting setting) {
        muteMic = setting.muteMic;
        trackCount = 2;
        if (muteMic) {
            trackCount = 1;
        }
        ELog.i(TAG, "init muxer trackCount:" + trackCount);
    }


    @Override
    public void onUpdateAudioMediaFormat(MediaFormat mediaFormat) {
        if (audioTrackReady || muteMic) return;
        ELog.i(TAG, "onUpdateAudioMediaFormat" + mediaFormat);
        audioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
        if (audioTrackIndex < 0) {
            ELog.e(TAG, "Add audio track failed");
            return;
        }
        audioTrackReady = true;
        ELog.i(TAG, "audio track has ready");
    }


    @Override
    public void onUpdateVideoMediaFormat(MediaFormat mediaFormat) {
        if (videoTrackReady) return;
        ELog.i(TAG, "onUpdateAudioMediaFormat" + mediaFormat);
        videoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
        if (videoTrackIndex < 0) {
            ELog.e(TAG, "Add video track failed");
            return;
        }
        if (!muteMic) {
            while (!audioTrackReady) {
                Thread.yield();
            }
        }
        videoTrackReady = true;
        ELog.i(TAG, "video track has ready");
    }

    @Override
    public void onEncodedFrame(EncodedImage frame) {
        if ((videoTrackReady && audioTrackReady) || (videoTrackReady && muteMic)) {
            start();
            mMediaMuxer.writeSampleData(videoTrackIndex, frame.buffer, frame.bufferInfo);
            ELog.i(TAG, "mux video data，index=" + frame.index
                    + " timeStamp:" + frame.bufferInfo.presentationTimeUs);
        }
    }


    @Override
    public void onEncodedAudio(EncodedAudio frame) {
        if (muxerStarted) {
            mMediaMuxer.writeSampleData(audioTrackIndex, frame.buffer, frame.bufferInfo);
            ELog.i(TAG, "mux audio data，timeStamp=" + frame.bufferInfo.presentationTimeUs);
        }
    }

    private void start() {
        if (muxerStarted) return;
        synchronized (Mp4Muxer.class) {
            if (!muxerStarted) {
                mMediaMuxer.start();
                muxerStarted = true;
            }
        }
    }

    @Override
    public void onVideoEncoderStop() {
        ELog.i(TAG, "onVideoEncoderStop. trackCount=" + trackCount);
        stopMuxer();
    }

    @Override
    public void onAudioEncoderStop() {
        ELog.i(TAG, "onAudioEncoderStop: trackCount=" + trackCount);
        stopMuxer();
    }

    private void stopMuxer() {
        synchronized (Mp4Muxer.class) {
            trackCount--;
            ELog.i(TAG, "stopMuxer trackCount:" + trackCount);
            if (trackCount <= 0) {
                release();
            }
        }
    }

    void release() {
        ELog.i(TAG, "start release mp4Muxer");
        try {
            if (mMediaMuxer != null) {
                mMediaMuxer.release();
            }
            videoTrackIndex = -1;
            audioTrackIndex = -1;
            muxerStarted = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        ELog.i(TAG, "release mp4Muxer success");
    }

}
