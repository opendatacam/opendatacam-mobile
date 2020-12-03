package com.opendatacam;

import android.app.Fragment;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

public class CameraActivity extends Fragment {

    private View view;

    public int width;
    public int height;
    public int x;
    public int y;

    private String appResourcesPackage;
    private TextureView viewFinder;

    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.BACK;

    // Constructor
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        appResourcesPackage = getActivity().getPackageName();

        // Inflate the layout for this fragment
        view = inflater.inflate(getResources().getIdentifier("camera_activity", "layout", appResourcesPackage), container, false);
        viewFinder = view.findViewById(R.id.view_finder);
        createCameraPreview();
        return view;
    }

    public void createCameraPreview() {

        CameraX.unbindAll();

        // CURRENT TRY, RENDER THE CAMERA PREVIEW of CameraX IN THE viewFinder box in the fragment from the capacitor preview plugin
        // Compile, but can't see the video... need to debug what is going wrong
        // See if the "overlay" is rendering, maybe by giving it a color
        // if so, then figure out why the SurfaceTexture isn't rendering the video frames.

        // 1. preview This is responsible to display the preview on the bottom left.. not sure we want to keep this
        // but seems it is the preview that is fed to the neural network.. that would make sense as the resolution is shrinked down

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(CAMERA_ID)
//                .setTargetAspectRatio(Rational.NEGATIVE_INFINITY)  // 宽高比
                .setTargetResolution(new Size(480, 640))  // 分辨率
                .build();

        Preview preview = new Preview(previewConfig);

        // TODO keep copying code from the YOLOv5 project.. but adapting where the camera display is using
        // the example from the cameraPreview capacitor plugin

        // This is responsible to display the preview on the bottom left.. not sure we want to keep this
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);

                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });
        //DetectAnalyzer detectAnalyzer = new DetectAnalyzer();
        //CameraX.bindToLifecycle((LifecycleOwner) this, preview, gainAnalyzer(detectAnalyzer));


        // THIS IS CODE FROM CameraPreview capacitor plugin
        // like in the example CameraPreview capacitor plugin
        // CameraPreview.java startCamera method

        // set a dimension to the fragment to the height and width of device
        // then create a container view with transparent background
        // the fragment beiing the CameraActivity class with the camera rendering into it
        // see how to do the same using the CameraX API



    }

    private void updateTransform() {
        Matrix matrix = new Matrix();
        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        float[] rotations = {0, 90, 180, 270};
        // Correct preview output to account for display rotation
        float rotationDegrees = rotations[viewFinder.getDisplay().getRotation()];

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix);
    }

    public void setRect(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
