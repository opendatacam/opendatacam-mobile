package com.opendatacam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import androidx.core.hardware.display.DisplayManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.getcapacitor.Bridge;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


// Code inspired by android examples apps:
// https://github.com/android/camera-samples/tree/main/CameraXBasic
// https://github.com/android/camera-samples/tree/main/CameraXTfLite

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

    private double RATIO_4_3_VALUE = 4.0 / 3.0;
    private double RATIO_16_9_VALUE = 16.0 / 9.0;

    RequestQueue requestQueue = null;

    private int currentRotation = 0;
    private DisplayManager mDisplayManager;

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {

        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (imageAnalysis != null) {
                currentRotation = previewView.getDisplay().getRotation();
                //System.out.println("CameraActivity Rotation changed: " + currentRotation);
                imageAnalysis.setTargetRotation(currentRotation);
                preview.setTargetRotation(currentRotation);
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {

        }
    };

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


        // Every time the orientation of device changes, update rotation for use cases
        mDisplayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(mDisplayListener, null);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                createCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getActivity()));


        requestQueue = Volley.newRequestQueue(getContext());

        return view;
    }

    private Integer aspectRatio(Integer width, Integer height) {
        double previewRatio = Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public void createCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {


        // Get screen metrics used to setup camera for full screen resolution
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);

        //Log.d("CameraActivity", "Screen metrics: " + metrics.widthPixels + " x " + metrics.heightPixels);
        //System.out.println("CameraActivity Screen metrics: " + metrics.widthPixels + " x " + metrics.heightPixels);

        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        //Log.d("CameraActivity", "Preview aspect ratio: " + screenAspectRatio);
        //System.out.println("CameraActivity Preview aspect ratio: " + screenAspectRatio);

        currentRotation = previewView.getDisplay().getRotation();

        //System.out.println("CameraActivity: Rotation init " + currentRotation);

        preview = new Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(currentRotation)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // We request aspect ratio but no resolution
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(currentRotation)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                long start = System.currentTimeMillis();

                // The roatationDegree value is computed by the imageAnalysis
                // class.. ImageProxy contains the raw sensor image data without any rotation
                // Depending on the current rotation of the device, it will compute the necessary
                // rotation to get it upright..
                detectOnModel(image, rotationDegrees);
                image.close();

                /*
                setTimeout(() -> {



                    }, 1000); */

                final long dur = System.currentTimeMillis() - start;
                //System.out.println(String.format(Locale.CHINESE, "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f",
                //        "tinyYOLO", height, width, dur / 1000.0, 1000.0f / dur));
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
                //System.out.println("CameraActivity Detecting on image that is:");
                //System.out.println(width + "x" + height);
                //System.out.println("CameraActivity rotate " + rotationDegrees);
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false);

                // To debug, take picture of the Bitmap sent to YOLO to see if correctly rotated
                //saveImage(bitmap, "frame"+ new Date().toString());

                // TODO PREVIEW THIS or save frame
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

        //Bundle result = new Bundle();
        //result.putString("jsonData", objectsDetectedJSON);
        //getParentFragmentManager().setFragmentResult("frameData", result);

        // TODO DO THIS ONLY WHEN NODE SERVER IS STARTED ?

        try {
            postData(new JSONArray(objectsDetectedJSON));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //isDetectingOnCamera.set(false);

        return objectsDetected;
    }

    // Post Request For JSONObject
    public void postData(JSONArray jsonArray) {

        // Enter the correct url for your api service site
        String url = "http://localhost:8080/updatewithnewframe";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArray,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        System.out.println("String Response : "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error getting response");
            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    public static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }
    

    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
