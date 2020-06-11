package com.gpufast.logger;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class FwLogImp {

    private static FwLogImp instance = null;
    private static ArrayList<String> levelArray = new ArrayList<>();
    private static SimpleDateFormat sdf;
    private static LogWriter logWriter;

    static void init(Context context, String sdkVersion) {
        if (instance == null) {
            sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
            LogEntity.init(context);
            logWriter = LogWriter.getInstance();
            instance = new FwLogImp();
        }

        LogEntity.getInstance().setVersion(sdkVersion);
        LogEntity.getInstance().setConsoleLogLevel(ELog.VERBOSE_LEVEL);
        LogEntity.getInstance().setMonitorLevel(ELog.INFO_LEVEL);
    }


    void setConsoleLogLevel(int level) {
        LogEntity.getInstance().setConsoleLogLevel(level);
    }

    void setMonitorLevel(int level) {
        LogEntity.getInstance().setMonitorLevel(level);
    }

    void writeLog(long tid, long timestamp, int level, String tag, String msg) {
        if (LogEntity.getInstance().getLogMode() != 0) {
            if (level <= LogEntity.getInstance().getConsoleLogLevel()) {
                showConsoleLog(level, tag, msg);
            }
        }
        if (level <= LogEntity.getInstance().getMonitorLevel()) {
            String gmtTime = sdf.format(new Date(timestamp));
            final String logStr = gmtTime + " " + tid + " " + levelArray.get(level) + " [" + tag + "] msg:" + msg;
            logWriter.writer(logStr);
        }
    }

//    private static boolean checkWriter(){
//        if(logWriter == null){
//            synchronized (FwLogImp.class){
//                if(logWriter == null){
//                    boolean permit = PermissionChecker.permitPermissions(context);
//                    if(permit){
//                        logWriter = LogWriter.getInstance();
//                        return true;
//                    }
//                    return false;
//                }
//            }
//        }
//        return true;
//    }


    private static void showConsoleLog(int level, String tag, String log) {
        tag = "[" + tag + "]";
        switch (level) {
            case ELog.ERROR_LEVEL:
                Log.e(tag, log);
                break;
            case ELog.WARN_LEVEL:
                Log.w(tag, log);
                break;
            case ELog.DEBUG_LEVEL:
                Log.d(tag, log);
                break;
            case ELog.INFO_LEVEL:
                Log.i(tag, log);
                break;
            case ELog.VERBOSE_LEVEL:
                Log.v(tag, log);
        }
    }


    void deInit() {
        logWriter.shutdown();
        logWriter = null;
        sdf = null;
        instance = null;
    }

    static {
        levelArray.add("N");
        levelArray.add("F");
        levelArray.add("E");
        levelArray.add("W");
        levelArray.add("I");
        levelArray.add("D");
        levelArray.add("V");
    }

    static FwLogImp getInstance() {
        return instance;
    }

}
