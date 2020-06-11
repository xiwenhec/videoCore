package com.gpufast.effectcamera;

import android.app.Application;

import com.gpufast.logger.FwLog;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FwLog.init(this,"0.0.1");
    }
}
