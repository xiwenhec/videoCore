package com.gpufast.recorder.hardware;


import java.io.IOException;

public interface MediaCodecWrapperFactory {
  MediaCodecWrapper createByCodecName(String name) throws IOException;
}