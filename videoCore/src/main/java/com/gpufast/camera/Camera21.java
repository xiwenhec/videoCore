package com.gpufast.camera;

/**
 * @author Sivin 2018/10/29
 * Description:Camera2 架构实现
 */
 class Camera21 implements ICamera {


    @Override
    public void setCameraParams(CameraParams params) {

    }

   @Override
   public void openCamera(int orientation) {

   }

   private boolean openFrontCamera() {


        return false;
    }

   private boolean openBackCamera() {


        return false;
    }

    @Override
    public void switchCamera() {

    }

    @Override
    public void startPreview() {

    }

    @Override
    public void stopPreview() {

    }

    @Override
    public void stopCamera() {

    }
}
