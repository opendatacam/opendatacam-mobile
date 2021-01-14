package com.opendatacam;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class YOLOv4
{
    public static native int init(AssetManager manager, boolean useGPU);
    public static native Bbox[] detect(Bitmap bitmap, double threshold, double nms_threshold);

    static {
        System.loadLibrary("yolov4");
    }
}