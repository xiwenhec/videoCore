package com.gpufast.effectcamera.recorder.ui;

import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.gpufast.effectcamera.BaseFragment;
import com.gpufast.effectcamera.R;
import com.gpufast.effectcamera.recorder.contract.RecorderContract;
import com.gpufast.effectcamera.recorder.presenter.RecorderPresenter;
import com.gpufast.recorder.RecordParams;

import java.util.Objects;

public class RecorderFragment extends BaseFragment implements RecorderContract.View, View.OnClickListener {
    private static final String TAG = "RecorderFragment";
    private SurfaceView mPreview;
    private RecorderPresenter mPresenter;
    private ImageView mStartRecorderBtn;
    private ImageView mSwitchCameraBtn;


    @Override
    protected int getLayoutId() {
        return R.layout.recoder_fragment_layout;
    }

    @Override
    protected void onInitView() {
        mPreview = findViewById(R.id.id_camera_preview);
        mSwitchCameraBtn = findViewById(R.id.id_switch_camera);
        mStartRecorderBtn = findViewById(R.id.id_start_recorder_btn);

        Objects.requireNonNull(mSwitchCameraBtn).setOnClickListener(this);
        Objects.requireNonNull(mStartRecorderBtn).setOnClickListener(this);

        mPresenter = new RecorderPresenter();
        initRecorderParams();
        mPresenter.attachView(this);
        mPresenter.init();
    }

    private void initRecorderParams() {

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        RecordParams.Builder builder = new RecordParams.Builder();
        builder.setVideoWidth(720)
                .setVideoHeight(1280)
                .setEnableHwEncoder(true)
                .setSavePath(path + "/a_test/test.mp4");
        mPresenter.setRecorderParameter(builder.build());
    }


    @Override
    public SurfaceView getPreview() {
        return mPreview;
    }

    @Override
    public void onStartRecorder() {

    }

    @Override
    public void onRecorderProgress(int time_s) {

    }

    @Override
    public void onRecorderFinish() {

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_switch_camera:
                mPresenter.switchCamera();
                break;
            case R.id.id_start_recorder_btn:
                if (mPresenter.isRecording()) {
                    mPresenter.stopRecorder();
                } else {
                    mPresenter.startRecorder();
                }
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPresenter != null)
            mPresenter.stopRecorder();
    }
}
