package com.opendatacam;

import android.Manifest;

import android.graphics.Color;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

    private CameraActivityLegacy fragment;
    private int containerViewId = 20;

    @PluginMethod()
    public void startObjectDetection(PluginCall call) {

        fragment = new CameraActivityLegacy(getBridge());

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