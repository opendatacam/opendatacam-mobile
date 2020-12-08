package com.opendatacam;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import java.util.Random;

public class Box {
    public float x,y,width,height;
    private int label;
    private float score;
    private static String[] labels={"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
            "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
            "hair drier", "toothbrush"};
    public Box(float x0,float y0, float x1, float y1, int label, float score){
        this.x = x0;
        this.y = y0;
        this.width = x1 - x0;
        this.height = y1 - y0;
        this.label = label;
        this.score = score;
    }

    public String getLabel(){
        return labels[label];
    }

    public float getScore(){
        return score;
    }

    public int getColor(){
        Random random = new Random(label);
        return Color.argb(255,random.nextInt(256),random.nextInt(256),random.nextInt(256));
    }

    @Override
    public String toString() {
        return "Box{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", label=" + label +
                ", score=" + score +
                '}';
    }
}