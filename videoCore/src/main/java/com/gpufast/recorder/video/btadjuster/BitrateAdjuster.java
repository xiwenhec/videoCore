package com.gpufast.recorder.video.btadjuster;

/** 用于调整硬件编解码器的码率. */
public interface BitrateAdjuster {
  /**
   * 设置目标码率（bps) 和帧率(fps)
   */
  void setTargets(int targetBitrateBps, int targetFps);

  /**
   * 报告指定大小的frame已经编码完成，如果码率需要调整，则返回true
   */
  void reportEncodedFrame(int size);

  /** 获取当前的码率. */
  int getAdjustedBitrateBps();

  /** 获取配置编码器的帧率. */
  int getCodecConfigFrameRate();
}