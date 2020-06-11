package com.gpufast.effectcamera.recorder.contract;

import android.view.SurfaceView;

import com.gpufast.recorder.RecordParams;

public interface RecorderContract {

    interface View{

        //设置预览界面
        SurfaceView getPreview();

        //开始录制回调
        void onStartRecorder();

        //录制时间回调
        void onRecorderProgress(int time_s);

        //结束录制回调
        void onRecorderFinish();


    }

    interface Presenter{

        //切换摄像头
        void switchCamera();

        //设置视频录制参数
        void setRecorderParameter(RecordParams params);

        //开始录制
        void startRecorder();

        //暂停录制
        void stopRecorder();

        //合成视频
        void jointVideo();

        boolean isRecording();
    }


}
