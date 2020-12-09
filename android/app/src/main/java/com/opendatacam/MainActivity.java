package com.opendatacam;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.util.ArrayList;

public class MainActivity extends BridgeActivity {

  static {
    System.loadLibrary("native-lib");
    System.loadLibrary("node");
  }

  //We just want one instance of node running in the background.
  public static boolean _startedNodeAlready=false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initializes the Bridge
    this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
      // Additional plugins you've installed go here
      add(CameraObjectDetection.class);



      //fragment = new CameraActivity();
      //fragment.startCamera();

    }});

    if( !_startedNodeAlready ) {
      _startedNodeAlready=true;
      new Thread(new Runnable() {
        @Override
        public void run() {
          startNodeWithArguments(new String[]{"node", "-e",
                  "var http = require('http'); " +
                          "var versions_server = http.createServer( (request, response) => { " +
                          "  response.end('Versions: ' + JSON.stringify(process.versions)); " +
                          "}); " +
                          "versions_server.listen(3000);"
          });
        }
      }).start();
    }
  }

  /**
   * A native method that is implemented by the 'native-lib' native library,
   * which is packaged with this application.
   */
  public native void startNodeWithArguments(String[] arguments);
}
