package com.gpufast.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Environment;

import java.io.File;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;

public class LogEntity {

    private static final String LOG_DIR = "effectCamera_log";
    private static final String APP_VERSION = "APP_VERSION";
    private static final String MONITOR_LEVEL = "MONITOR_LEVEL";
    private static final String CONSOLE_LEVEL = "CONSOLE_LEVEL";

    private static LogEntity instance;

    private static SharedPreferences sharedPreferences;

    //是否是debug
    private final boolean isDebugMode;

    //日志文件夹路径
    private String logDir;

    //sdk版本
    private String version;

    //控制台日志级别
    private int consoleLogLevel;

    //监控日志级别
    private int monitorLevel;

    static void init(Context context) {
        sharedPreferences = context.getSharedPreferences("FwLog", 0);
        instance = new LogEntity(context);
    }

    private LogEntity(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        isDebugMode = info != null && (info.flags & FLAG_DEBUGGABLE) != 0;
        try {
            logDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + LOG_DIR;
        } catch (Exception e) {
            FwLog.write(ELog.ERROR_LEVEL, "L-crash_main_ept-E", FwLog.stackToString(e));
        }
    }

    static LogEntity getInstance() {
        if (instance == null) {
            throw new RuntimeException("LogEntity.init() has not been called.");
        } else {
            return instance;
        }
    }

    String getLogDir() {
        return logDir;
    }

    String getVersion() {
        if (version == null) {
            version = sharedPreferences.getString(APP_VERSION, null);
        }
        return version;
    }

    void setVersion(String version) {
        sharedPreferences.edit().putString(APP_VERSION, version).apply();
        this.version = version;
    }

    int getLogMode() {
        return isDebugMode ? 1 : 0;
    }

    void setConsoleLogLevel(int level) {
        sharedPreferences.edit().putInt(CONSOLE_LEVEL, level).apply();
        consoleLogLevel = level;
    }

    int getConsoleLogLevel() {
        if (consoleLogLevel == 0) {
            consoleLogLevel = sharedPreferences.getInt(MONITOR_LEVEL, 0);
        }
        return consoleLogLevel;
    }

    void setMonitorLevel(int level) {
        sharedPreferences.edit().putInt(MONITOR_LEVEL, level).apply();
        monitorLevel = level;
    }

    int getMonitorLevel() {
        if (monitorLevel == 0) {
            monitorLevel = sharedPreferences.getInt(MONITOR_LEVEL, 0);
        }
        return monitorLevel;
    }

}
