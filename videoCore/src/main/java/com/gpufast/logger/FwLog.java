package com.gpufast.logger;

import android.content.Context;
import android.util.Log;

public class FwLog {
    private static FwLogImp instance;

    /**
     * 初始化FwLog
     *
     * @param context    context
     * @param sdkVersion version
     */
    public static void init(Context context, String sdkVersion) {
        FwLogImp.init(context, sdkVersion);
        instance = FwLogImp.getInstance();
    }

    /**
     * 设置控制台显示日志级别
     * @param level level
     */
    public static void setConsoleLogLevel(int level) {
        if (instance != null) {
            instance.setConsoleLogLevel(level);
        }
    }

    /**
     * 设置写入文件日志级别
     * @param level level
     */
    public static void setMonitorLevel(int level) {
        if (instance != null) {
            instance.setMonitorLevel(level);
        }
    }

    static void write(int level, String tag, String msg) {
        write(Thread.currentThread().getId(), System.currentTimeMillis(), level, tag, msg);
    }

    private static void write(long tid, long timestamp, int level, String tag, String msg) {
        if (instance != null) {
            instance.writeLog(tid, timestamp, level, tag, msg);
        }
    }

    static String stackToString(Throwable error) {
        return Log.getStackTraceString(error).replaceAll("\n", "\\\\n");
    }

    public static void deinit() {
        if (instance != null) {
            instance.deInit();
            instance = null;
        }
    }
}
