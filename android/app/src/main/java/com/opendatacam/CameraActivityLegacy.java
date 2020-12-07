package com.opendatacam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.getcapacitor.Bridge;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class CameraActivityLegacy extends Fragment {

    private View view;

    public int width;
    public int height;
    public int x;
    public int y;

    private String appResourcesPackage;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private AtomicBoolean isDetectingOnCamera = new AtomicBoolean(false);

    ExecutorService cameraExecutor;

    private double threshold = 0.3, nms_threshold = 0.7;

    private long startTime = 0;
    private long endTime = 0;

    double total_fps = 0;
    int fps_count = 0;

    // Constructor
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        appResourcesPackage = getActivity().getPackageName();
        // Inflate the layout for this fragment
        view = inflater.inflate(getResources().getIdentifier("camera_activity", "layout", appResourcesPackage), container, false);
        previewView = view.findViewById(R.id.preview_view);

        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());

        cameraExecutor = Executors.newSingleThreadExecutor();

        YOLOv4.init(getActivity().getAssets(), 0, false);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                createCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getActivity()));

        return view;
    }

    public void createCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        //.setTargetResolution(new Size(480, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                // insert your code here.
                System.out.println("imageAnalysis loop");

                startTime = System.currentTimeMillis();

                detectOnModel(image);
                image.close();

                endTime = System.currentTimeMillis();
                long dur = endTime - startTime;
                float fps = (float) (1000.0 / dur);
                total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                fps_count++;
                System.out.println(String.format(Locale.ENGLISH,
                        "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
                        "yolov4 tiny", height, width, dur / 1000.0, fps, (float) total_fps / fps_count));
            }
        });

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();


        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

    }

    // TODO add on destroy view

    public void setRect(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        System.out.println("width");
        System.out.println(width);
        System.out.println("height");
        System.out.println(height);
    }

    private byte[] imagetToNV21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ImageProxy.PlaneProxy y = planes[0];
        ImageProxy.PlaneProxy u = planes[1];
        ImageProxy.PlaneProxy v = planes[2];
        ByteBuffer yBuffer = y.getBuffer();
        ByteBuffer uBuffer = u.getBuffer();
        ByteBuffer vBuffer = v.getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        byte[] nv21 = imagetToNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private void detectOnModel(ImageProxy image) {
        /*
        if (isDetectingOnCamera.get()) {
            return;
        }
        isDetectingOnCamera.set(true);
        */

        final Bitmap bitmapsrc = imageToBitmap(image);

        /*
        if (cameraExecutor == null) {
            isDetectingOnCamera.set(false);
            return;
        } */

        cameraExecutor.execute(new Runnable() {
            @Override
            public void run() {
                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height);
                System.out.println("Detect loop");
                //isDetectingOnCamera.set(false);
                detect(bitmap);
            }
        });
    }

    protected Box[] detect(Bitmap image) {
        Box[] objectsDetected = null;

        System.out.println("detecting on frame using YOLOv4.detect()");
        // Here it fails
        objectsDetected = YOLOv4.detect(image, threshold, nms_threshold);

        System.out.println("detecting on frame sucess return result");
        System.out.println(Arrays.toString(objectsDetected));

        Gson gson = new Gson();
        String objectsDetectedJSON = gson.toJson(objectsDetected);

        Bundle result = new Bundle();
        result.putString("jsonData", objectsDetectedJSON);
        getParentFragmentManager().setFragmentResult("frameData", result);


        //isDetectingOnCamera.set(false);

        return objectsDetected;
    }

    // TODO on destroy
}
