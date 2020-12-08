package com.opendatacam;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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


public class CameraActivity extends Fragment {

    private View view;

    public int width;
    public int height;
    public int x;
    public int y;

    private ImageAnalysis imageAnalysis = null;
    private Preview preview = null;

    private String appResourcesPackage;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private AtomicBoolean isDetectingOnCamera = new AtomicBoolean(false);

    ExecutorService cameraExecutor;

    private double threshold = 0.3, nms_threshold = 0.7;

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

        // See: https://developer.android.com/training/camerax/orientation-rotation
        OrientationEventListener mOrientationListener = new OrientationEventListener(
                getContext()) {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onOrientationChanged(int orientation) {

                //System.out.println(orientation);

                if (imageAnalysis != null) {
                    if (orientation >= 45 && orientation < 135) {
                        System.out.println("Change imageAnalysis rotation");
                        imageAnalysis.setTargetRotation(Surface.ROTATION_270);
                        preview.setTargetRotation(Surface.ROTATION_270);
                    } else if (orientation >= 135 && orientation < 225) {
                        System.out.println("Change imageAnalysis rotation");
                        imageAnalysis.setTargetRotation(Surface.ROTATION_180);
                        preview.setTargetRotation(Surface.ROTATION_180);
                    } else if (orientation >= 225 && orientation < 315) {
                        System.out.println("Change imageAnalysis rotation");
                        imageAnalysis.setTargetRotation(Surface.ROTATION_90);
                        preview.setTargetRotation(Surface.ROTATION_90);
                    } else {
                        System.out.println("Change imageAnalysis rotation");
                        imageAnalysis.setTargetRotation(Surface.ROTATION_0);
                        preview.setTargetRotation(Surface.ROTATION_0);
                    }
                }

            }

        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        return view;
    }

    public void createCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {

        preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                long start = System.currentTimeMillis();

                detectOnModel(image, rotationDegrees);
                image.close();

                final long dur = System.currentTimeMillis() - start;
                System.out.println(String.format(Locale.CHINESE, "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f",
                        "tinyYOLO", height, width, dur / 1000.0, 1000.0f / dur));
            }
        });

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

    }

    // TODO add on destroy view

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

    private void detectOnModel(ImageProxy image, final int rotationDegrees) {
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
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false);
                //isDetectingOnCamera.set(false);
                detect(bitmap);
            }
        });
    }

    protected Box[] detect(Bitmap image) {
        Box[] objectsDetected = null;

        //System.out.println("detecting on frame using YOLOv4.detect()");

        objectsDetected = YOLOv4.detect(image, threshold, nms_threshold);

        //System.out.println("detecting on frame sucess return result");
        //System.out.println(Arrays.toString(objectsDetected));

        Gson gson = new Gson();
        String objectsDetectedJSON = gson.toJson(objectsDetected);

        Bundle result = new Bundle();
        result.putString("jsonData", objectsDetectedJSON);
        getParentFragmentManager().setFragmentResult("frameData", result);


        //isDetectingOnCamera.set(false);

        return objectsDetected;
    }

}
