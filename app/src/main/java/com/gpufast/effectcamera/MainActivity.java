package com.gpufast.effectcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.gpufast.effectcamera.recorder.ui.RecorderActivity;
import com.gpufast.logger.ELog;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private Button mStartCameraBtn;
    private RxPermissions rxPermissions;
    private int grantNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        rxPermissions = new RxPermissions(this);
    }

    private void initView() {
        mStartCameraBtn = findViewById(R.id.start_camera_btn);
    }

    private void initEvent() {
        mStartCameraBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_camera_btn:
                startCameraActivity();
                break;
        }

    }

    @SuppressLint("CheckResult")
    private void startCameraActivity() {
        grantNum = 0;
        rxPermissions.requestEach(Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe(permission -> {
            if (permission.granted) {
                ELog.i(TAG, "permission granted:" + permission.name);
                grantNum++;
                if (grantNum == 4) {
                    ELog.i(TAG, "all permission is granted:");
                    Intent intent = new Intent(MainActivity.this, RecorderActivity.class);
                    startActivity(intent);
                }
            } else if (permission.shouldShowRequestPermissionRationale) {

            } else {

            }
        });
    }
}
