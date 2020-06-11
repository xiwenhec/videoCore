package com.gpufast.camera;

import android.hardware.Camera;

import com.gpufast.logger.ELog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Sivin 2018/3/24
 */
class CameraUtils {

    private static final String TAG = "CameraUtils";

    /**
     * 根据要显示的宽度和告诉，找一个大于改尺寸最小组合。 如果发现尺寸对不上，就进行裁剪。
     *
     * @param list         相机支持的尺寸列表
     * @param targetWidth  预览视频view的宽度
     * @param targetHeight 预览视频的高度
     *
     * @return Camera.Size
     */
    static Camera.Size chooseOptimalSize(List<Camera.Size> list, int targetWidth, int targetHeight) {
        Collections.sort(list, new CameraSizeComparator());
        float rate = targetWidth * 1.0f / targetHeight;
        Camera.Size resultSize = null;
        for (Camera.Size size : list) {
            if ((size.height >= targetWidth) && equalRate(size, rate)) {
                resultSize = size;
                break;
            }
        }
        if (resultSize == null) {
            throw new RuntimeException("can not find target size :targetWidth =" + targetWidth);
        } else {
            ELog.i(TAG, "find targetSize :width=" + resultSize.height + " height=" + resultSize.width);
        }

        return resultSize;
    }

    /**
     * 寻找最合适的fps 算法首先找到和wantMaxFps最接近的，然后找和wantMinFps最接近的
     *
     * @param wantMinFps wantMinFps
     * @param wantMaxFps wantMaxFps
     *
     * @return result fps range
     */
    static int[] chooseOptimalFps(List<int[]> fpsRange, int wantMinFps, int wantMaxFps) {
        if (fpsRange == null)
            return null;
        if (fpsRange.size() == 0)
            return null;

        wantMaxFps = wantMaxFps * 1000;
        wantMinFps = wantMinFps * 1000;

        //初始化offset
        int minOffset = Math.abs(wantMaxFps - fpsRange.get(0)[1]);
        int dstMaxFps = fpsRange.get(0)[1];

        for (int i = 0; i < fpsRange.size(); i++) {
            int[] fps = fpsRange.get(i);
            int offset = Math.abs(wantMaxFps - fps[1]);
            if (offset < minOffset) {
                dstMaxFps = fpsRange.get(i)[1];
                minOffset = offset;
                if (minOffset == 0){
                    break;
                }
            }
        }
        //存放所有可能满足条件的fpsRange index
        List<int[]> fpsRangeIndexList = new ArrayList<>();
        for (int i = 0; i < fpsRange.size(); i++) {
            if (dstMaxFps == fpsRange.get(i)[1]) {
                fpsRangeIndexList.add(fpsRange.get(i));
            }
        }

        if (fpsRangeIndexList.size() == 0) {
            ELog.e(CameraUtils.class, "can't find any fps to match wantMaxFps:" + wantMaxFps);
            return null;
        }

        int[] ret = null;
        //重新初始化minOffset
        minOffset = Math.abs(fpsRangeIndexList.get(0)[0] - wantMinFps);

        for (int i = 0; i < fpsRangeIndexList.size(); i++) {
            int offset = Math.abs(fpsRangeIndexList.get(i)[0] - wantMinFps);
            if (offset < minOffset) {
                ret = fpsRangeIndexList.get(i);
                minOffset = offset;
                if(minOffset == 0){
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * 从小到大排序
     */
    static class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Integer.compare(lhs.width, rhs.width);
        }
    }

    private static boolean equalRate(Camera.Size s, float rate) {
        float r = (float)(s.height) / (float)(s.width);
        return Math.abs(r - rate) <= 0.03;
    }

}
