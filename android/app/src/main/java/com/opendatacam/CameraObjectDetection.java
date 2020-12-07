package com.opendatacam;

import android.Manifest;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

@NativePlugin(
    permissions={
            Manifest.permission.CAMERA
    }
)
public class CameraObjectDetection extends Plugin {

    private CameraActivity fragment;
    private int containerViewId = 20;

    @PluginMethod()
    public void startObjectDetection(PluginCall call) {

        fragment = new CameraActivity();

        // 1. Start camera preview if not start
        bridge.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Display defaultDisplay = getBridge().getActivity().getWindowManager().getDefaultDisplay();
                final Point size = new Point();
                defaultDisplay.getSize(size);
                // Create container view
                FrameLayout containerView = getBridge().getActivity().findViewById(containerViewId);
                if(containerView == null){
                    containerView = new FrameLayout(getActivity().getApplicationContext());
                    containerView.setId(containerViewId);
                    containerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    getBridge().getWebView().setBackgroundColor(Color.TRANSPARENT);
                    ((ViewGroup)getBridge().getWebView().getParent()).addView(containerView);

                    // to back
                    getBridge().getWebView().getParent().bringChildToFront(getBridge().getWebView());

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.add(containerView.getId(), fragment);
                    fragmentTransaction.commit();

                    // Listen for frameData
                    fragmentManager.setFragmentResultListener("frameData", getActivity(), new FragmentResultListener() {
                        @Override
                        public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                            // We use a String here, but any type that can be put in a Bundle is supported
                            String frameDataJSONString = bundle.getString("jsonData");
                            JSObject frameData = new JSObject();
                            frameData.put("frameData", frameDataJSONString);
                            // Do something with the result
                            notifyListeners("frameData", frameData);
                        }
                    });


                    call.success();
                } else {
                    call.reject("camera already started");
                }
            }
        });
    }

    @PluginMethod()
    public void stopObjectDetection(PluginCall call) {

        // Stop camera preview / stop yolo / stop the app ?

        // More code here...
        call.success();
    }
}