package com.gpufast.recorder.video.btadjuster;


/**
 * BitrateAdjuster that adjusts the bitrate to compensate for changes in the framerate.  Used with
 * hardware codecs that assume the framerate never changes.
 */
public class FrameRateBitrateAdjuster extends BaseBitrateAdjuster {
  private static final int INITIAL_FPS = 30;

  @Override
  public void setTargets(int targetBitrateBps, int targetFps) {
    if (this.targetFps == 0) {
      // FrameRate-based bitrate adjustment always initializes to the same framerate.
      targetFps = INITIAL_FPS;
    }
    super.setTargets(targetBitrateBps, targetFps);

    this.targetBitrateBps = this.targetBitrateBps * INITIAL_FPS / this.targetFps;
  }

  @Override
  public int getCodecConfigFrameRate() {
    return INITIAL_FPS;
  }
}
