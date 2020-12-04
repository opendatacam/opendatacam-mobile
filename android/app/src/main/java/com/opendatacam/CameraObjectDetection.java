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

    private CameraActivity fragment;
    private int containerViewId = 20;

    @PluginMethod()
    public void startObjectDetection(PluginCall call) {

        final Integer x = call.getInt("x", 0);
        final Integer y = call.getInt("y", 0);

        fragment = new CameraActivity();

        // 1. Start camera preview if not start
        bridge.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Set fragment to the right dimensions
                DisplayMetrics metrics = getBridge().getActivity().getResources().getDisplayMetrics();
                // offset
                int computedX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, metrics);
                int computedY = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, y, metrics);

                // size
                int computedWidth;
                int computedHeight;
                int computedPaddingBottom = 0;

                Display defaultDisplay = getBridge().getActivity().getWindowManager().getDefaultDisplay();
                final Point size = new Point();
                defaultDisplay.getSize(size);

                computedWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size.x, metrics);
                computedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size.y, metrics) - computedPaddingBottom;

                fragment.setRect(computedX, computedY, computedWidth, computedHeight);

                // Create container view
                FrameLayout containerView = getBridge().getActivity().findViewById(containerViewId);
                if(containerView == null){
                    containerView = new FrameLayout(getActivity().getApplicationContext());
                    containerView.setId(containerViewId);

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

        // 2. Apply detect() on each frame

        // 3. Return data from each frame
        // Send each frameData back to js with events
        // Will listen from js code with something like this
        // Plugins.CameraObjectDetection.addListener('frameData', (data) => {
        //  console.log('frameData was fired');
        //});
        // Emit from java with something like:
        //JSObject ret = new JSObject();
        //ret.put("value", "some value");
        //notifyListeners("frameData", ret);

        call.success();
    }

    @PluginMethod()
    public void stopObjectDetection(PluginCall call) {

        // Stop camera preview / stop yolo / stop the app ?

        // More code here...
        call.success();
    }
}