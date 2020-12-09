package com.opendatacam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
          //The path where we expect the node project to be at runtime.
          String nodeDir=getApplicationContext().getFilesDir().getAbsolutePath()+"/nodejs-project";
          if (wasAPKUpdated()) {
            //Recursively delete any existing nodejs-project.
            File nodeDirReference=new File(nodeDir);
            if (nodeDirReference.exists()) {
              deleteFolderRecursively(new File(nodeDir));
            }
            //Copy the node project from assets into the application's data path.
            copyAssetFolder(getApplicationContext().getAssets(), "nodejs-project", nodeDir);

            saveLastUpdateTime();
          }
          startNodeWithArguments(new String[]{"node",
                  nodeDir+"/main.js"
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

  private static boolean deleteFolderRecursively(File file) {
    try {
      boolean res=true;
      for (File childFile : file.listFiles()) {
        if (childFile.isDirectory()) {
          res &= deleteFolderRecursively(childFile);
        } else {
          res &= childFile.delete();
        }
      }
      res &= file.delete();
      return res;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
    try {
      String[] files = assetManager.list(fromAssetPath);
      boolean res = true;

      if (files.length==0) {
        //If it's a file, it won't have any assets "inside" it.
        res &= copyAsset(assetManager,
                fromAssetPath,
                toPath);
      } else {
        new File(toPath).mkdirs();
        for (String file : files)
          res &= copyAssetFolder(assetManager,
                  fromAssetPath + "/" + file,
                  toPath + "/" + file);
      }
      return res;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
    InputStream in = null;
    OutputStream out = null;
    try {
      in = assetManager.open(fromAssetPath);
      new File(toPath).createNewFile();
      out = new FileOutputStream(toPath);
      copyFile(in, out);
      in.close();
      in = null;
      out.flush();
      out.close();
      out = null;
      return true;
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static void copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
  }

  private boolean wasAPKUpdated() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
    long previousLastUpdateTime = prefs.getLong("NODEJS_MOBILE_APK_LastUpdateTime", 0);
    long lastUpdateTime = 1;
    try {
      PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
      lastUpdateTime = packageInfo.lastUpdateTime;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return (lastUpdateTime != previousLastUpdateTime);
  }

  private void saveLastUpdateTime() {
    long lastUpdateTime = 1;
    try {
      PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
      lastUpdateTime = packageInfo.lastUpdateTime;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("NODEJS_MOBILE_APK_LastUpdateTime", lastUpdateTime);
    editor.commit();
  }
}
