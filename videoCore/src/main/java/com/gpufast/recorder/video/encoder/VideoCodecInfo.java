package com.gpufast.recorder.video.encoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VideoCodecInfo {

    public static final String H264_PROFILE = "profile";
    public static final String VALUE_BASE_LINE = "baseline";
    public static final String VALUE_main = "main";
    public static final String VALUE_height = "height";

    public enum Profile {
        BASE_LINE, MAIN, HHEIGHT
    }


    public final String name;
    public final Map<String, String> params;

    public VideoCodecInfo(String codecName, Profile profile) {
        this.name = codecName;
        this.params = new HashMap<>();
        switch (profile) {
            case BASE_LINE:
                params.put(H264_PROFILE, VALUE_BASE_LINE);
                break;
            case MAIN:
                params.put(H264_PROFILE, VALUE_main);
            case HHEIGHT:
                params.put(H264_PROFILE, VALUE_height);
            default:
                params.put(H264_PROFILE, VALUE_BASE_LINE);
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof VideoCodecInfo))
            return false;

        VideoCodecInfo otherInfo = (VideoCodecInfo) obj;
        return name.equalsIgnoreCase(otherInfo.name) && params.equals(otherInfo.params);
    }

    @Override
    public int hashCode() {
        Object[] values = {name.toUpperCase(Locale.ROOT), params};
        return Arrays.hashCode(values);
    }

    String getName() {
        return name;
    }

    Map getParams() {
        return params;
    }
}