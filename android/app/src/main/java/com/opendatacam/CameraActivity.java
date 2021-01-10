package com.opendatacam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
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
import androidx.core.app.ActivityCompat;
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
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginRequestCodes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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


    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationAvailable;
    private Location lastLocationOfDevice;

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


        System.out.println("REQUEST PERMISSIONS");

        // Init location watcher
        if (ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            System.out.println("PERMISSIONS GRANTED");
            watchLocationUpdates();
        }
        else {
            // You can directly ask for the permission.
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    PluginRequestCodes.GEOLOCATION_REQUEST_PERMISSIONS);
            System.out.println("REQUEST PERMISSIONS");
        }

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
                        System.out.println("String Response : " + response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error getting response");
            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }


    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name + ".jpg";
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

    private void watchLocationUpdates() {
        clearLocationUpdates();
        boolean enableHighAccuracy = true;
        int timeout = 10000;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setMaxWaitTime(timeout);
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
        locationRequest.setPriority(priority);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                System.out.println("LOCATION : onLocationResult");
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation == null) {
                    System.out.println("LOCATION : location unavailable");
                    isLocationAvailable = false;
                } else {
                    isLocationAvailable = true;
                    lastLocationOfDevice = lastLocation;
                    System.out.println("LOCATION : got location" + lastLocation.getLatitude() + "," + lastLocation.getLongitude());
                    JSONArray locationData = new JSONArray();
                    try {
                        locationData.put(lastLocation.getLatitude());
                        locationData.put(lastLocation.getLongitude());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    postCameraLocationData(locationData);

                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability availability) {
                System.out.println("LOCATION : onLocationAvailability");
                if (!availability.isLocationAvailable()) {
                    System.out.println("LOCATION : Location unavailable");
                    isLocationAvailable = false;
                    clearLocationUpdates();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // shouldn't pass here as we already checked permissions
            return;
        }
        System.out.println("LOCATION : requestLocationUpdates");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void clearLocationUpdates() {
        if (locationCallback != null) {
            System.out.println("LOCATION : clearLocationUpdates");
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PluginRequestCodes.GEOLOCATION_REQUEST_PERMISSIONS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    watchLocationUpdates();
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }


    public void postCameraLocationData(JSONArray jsonArray) {

        // Enter the correct url for your api service site
        String url = "http://localhost:8080/updatecameralocation";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArray,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        System.out.println("String Response : " + response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error getting response");
            }
        });
        requestQueue.add(jsonArrayRequest);
    }
}






