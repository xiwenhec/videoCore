package com.gpufast.recorder;

import android.os.Handler;

import com.gpufast.recorder.audio.AudioProcessor;

public abstract class BaseWorker implements IRecorder {

    protected static abstract class BaseWorkerThread extends Thread implements IRecorder{


        @Override
        public void sendVideoFrame(int textureId, int srcWidth, int srcHeight) {
        }

        @Override
        public void setRecordListener(RecordListener listener) {

        }
        @Override
        public void setAudioProcessor(AudioProcessor callback) {

        }

    }


    protected static abstract class BaseWorkerHandler extends Handler implements IRecorder{



        @Override
        public void sendVideoFrame(int textureId, int srcWidth, int srcHeight) {
        }

        @Override
        public void setRecordListener(RecordListener listener) {
        }
        @Override
        public void setAudioProcessor(AudioProcessor callback) {
        }

        @Override
        public boolean isRecording() {
            return false;
        }
    }

}
