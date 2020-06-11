package com.gpufast.logger;

/**
 * @author Sivin 2019/6/7
 */
public class ELog {

    static final int ERROR_LEVEL = 1;
    static final int WARN_LEVEL = 2;
    static final int DEBUG_LEVEL = 3;
    static final int INFO_LEVEL = 4;
    static final int VERBOSE_LEVEL = 5;

    private static final String ELOG_TAG = "EffectLib:";

    private ELog() {
        throw new UnsupportedOperationException();
    }


    public static void e(Class<?> cls, String msg) {
        if (cls == null) return;
        e(cls.getSimpleName(), msg);
    }

    public static void e(String tag, String msg) {
        log(tag, ELOG_TAG + msg, ERROR_LEVEL);
    }


    public static void w(Class<?> cls, String msg) {
        if (cls == null) return;
        w(cls.getSimpleName(), msg);
    }

    public static void w(String tag, String msg) {
        log(tag, ELOG_TAG + msg, WARN_LEVEL);
    }



    public static void d(Class<?> cls, String msg) {
        if (cls == null) return;
        d(cls.getSimpleName(), msg);
    }

    public static void d(String tag, String msg) {
        log(tag, ELOG_TAG + msg, DEBUG_LEVEL);
    }



    public static void i(Class<?> cls, String msg) {
        if (cls == null) return;
        i(cls.getSimpleName(), msg);
    }

    public static void i(String tag, String msg) {
        log(tag, ELOG_TAG + msg, INFO_LEVEL);
    }



    public static void v(Class<?> cls, String msg) {
        v(cls.getSimpleName(), msg);
    }

    public static void v(String tag, String msg) {
        log(tag, ELOG_TAG + msg, VERBOSE_LEVEL);
    }

    private static void log(String tag, String msg, int level) {
        FwLog.write(level, tag, msg);
    }
}
