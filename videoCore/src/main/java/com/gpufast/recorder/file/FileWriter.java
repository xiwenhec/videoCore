package com.gpufast.recorder.file;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.gpufast.logger.ELog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
@SuppressWarnings("unused")
public class FileWriter extends Thread {
    private static final String TAG = "FileWriter";
    private WriteHandler mHandler;
    private FileOutputStream os;
    private FileChannel dstChannel;
    //目标路径
    private String dstPath;

    public FileWriter(String dstPath) {
        this.dstPath = dstPath;
    }

    @SuppressWarnings("unused")
    public void startWrite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel_up26(dstPath);
        } else {
            createChannel_low26(dstPath);
        }
        if (dstChannel != null) {
            start();
        }
    }

    /**
     * 检查文件合法性
     *
     * @param file file
     *
     * @return true :合法，false:不合法
     */
    @SuppressWarnings("all")
    private boolean checkoutFile(File file) {
        //非空检测
        if (file == null)
            return false;

        //必须是文件，不能是目录
        if (file.isDirectory())
            return false;

        //文件已经存在，则删除掉文件，重新写入
        if (file.exists()) {
            return file.delete();
        }

        //文件不存在，检测文件是否具有多级目录
        //如果是多级目录，则判断目录是否已经创建，
        //如果没有创建，则为文件创建多级目录。
        File parentFile = file.getParentFile();
        if (parentFile.isFile()){ //出现异常错误。
            return false;
        }

        if (parentFile.exists()){ //目录已创建
            return true;
        }

        return parentFile.mkdirs();
    }

    private void createChannel_low26(String dstPath) {
        try {
            File file = new File(dstPath);
            if (!checkoutFile(file)) {
                ELog.e(TAG, "can't get file :" + dstPath);
                return;
            }
            os = new FileOutputStream(file);
            dstChannel = os.getChannel();
        } catch (FileNotFoundException e) {
            ELog.e(TAG, "create channel failed");
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel_up26(String dstPath) {
        try {
            File file = new File(dstPath);
            if (!checkoutFile(file)) {
                ELog.e(TAG, "can't get file :" + dstPath);
                return;
            }
            dstChannel = FileChannel
                .open(Paths.get(dstPath), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        } catch (IOException e) {
            ELog.e(TAG, "create channel failed");
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new WriteHandler(this);
        ELog.d(TAG, "fileWriter start");
        Looper.loop();
        release();
    }

    private void release() {

        ELog.i(TAG, "fileWriter stop");

        if (dstChannel != null) {
            try {
                dstChannel.close();
                dstChannel = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (os != null) {
            try {
                os.close();
                os = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mHandler = null;
    }

    @SuppressWarnings("unused")
    public void writeToFile(ByteBuffer data) {
        if (mHandler != null) {
            mHandler.sendToWriteFile(data);
        }
    }
    @SuppressWarnings("unused")
    public void stopWrite() {
        if (mHandler != null) {
            mHandler.stopWrite();
        }
    }

    private static class WriteHandler extends Handler {

        private static final int MSG_WRITE_FILE = 0x001;
        private static final int MSG_STOP_WRITE = 0x002;

        private WeakReference<FileWriter> mWeakFileWriter;

        WriteHandler(FileWriter writer) {
            mWeakFileWriter = new WeakReference<>(writer);
        }

        void sendToWriteFile(ByteBuffer data) {
            sendMessage(obtainMessage(MSG_WRITE_FILE, data));
        }

        void stopWrite() {
            sendMessage(obtainMessage(MSG_STOP_WRITE));
        }

        @Override
        public void handleMessage(Message msg) {

            if (msg.what == MSG_WRITE_FILE) {
                FileWriter fileWriter = mWeakFileWriter.get();
                if (fileWriter == null)
                    return;
                ByteBuffer data = (ByteBuffer)msg.obj;
                try {
                    ELog.d(FileWriter.class, "write data =" + data.limit());
                    fileWriter.dstChannel.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (msg.what == MSG_STOP_WRITE) {
                getLooper().quit();
            }
        }
    }

}
