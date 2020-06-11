package com.gpufast.recorder.video.btadjuster;

public class BaseBitrateAdjuster implements BitrateAdjuster {
    protected int targetBitrateBps;
    protected int targetFps;
    @Override
    public void setTargets(int targetBitrateBps, int targetFps) {
        this.targetBitrateBps = targetBitrateBps;
        this.targetFps = targetFps;
    }

    @Override
    public void reportEncodedFrame(int size) {

    }

    @Override
    public int getAdjustedBitrateBps() {
        return targetBitrateBps;
    }

    @Override
    public int getCodecConfigFrameRate() {
        return targetFps;
    }
}