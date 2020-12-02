package com.opendatacam;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

@NativePlugin()
public class CameraObjectDetection extends Plugin {

    @PluginMethod()
    public void startObjectDetection(PluginCall call) {
        // 1. Start camera preview if not started

        // use intend
        //Intent intent = new Intent(Intent.ACTION_VIEW);
        //getActivity().startActivity(intent);
        //

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