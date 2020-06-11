package com.gpufast.recorder.muxer;

import com.gpufast.recorder.RecordParams;

public class Mp4MuxerCreator {

    public static Mp4Muxer create(RecordParams params) {
        IMediaMuxer.Setting muxerSetting = createMuxerSetting(params);
        return new Mp4Muxer(muxerSetting);
    }


    private static IMediaMuxer.Setting createMuxerSetting(RecordParams params) {
        if (params == null) return null;
        return new IMediaMuxer.Setting(params.getSavePath(), params.isMuteMic());
    }

}
