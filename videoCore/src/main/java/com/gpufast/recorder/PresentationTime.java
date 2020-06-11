package com.gpufast.recorder;

public class PresentationTime {
    public long presentationTimeNs;
    private long timestamp;

    public PresentationTime(int fps) {
        this.presentationTimeNs = 0L;
        this.timestamp = 0L;
    }

    public void start() {
        timestamp = System.nanoTime();
    }

    public void record() {
        presentationTimeNs = (System.nanoTime() - timestamp);
    }
}
