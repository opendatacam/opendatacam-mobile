package com.opendatacam;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends BridgeActivity {

  private static final int PERMISSION_REQUEST_CODE = 200;

  static {
    System.loadLibrary("nodejsmobile");
    System.loadLibrary("node");
  }

  //We just want one instance of node running in the background.
  public static boolean _startedNodeAlready=false;

  private CameraActivity fragment;
  private int containerViewId = 20;

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

    if (checkPermission()) {
      mainlogic();
      attachDownloadManagerToWebView(getBridge().getWebView());
    } else {
      requestPermission();
    }
  }

  public void mainlogic() {
    // Start YOLO and camera preview
    // Create container view
    fragment = new CameraActivity();
    FrameLayout containerView = getBridge().getActivity().findViewById(containerViewId);
    if(containerView == null) {
      containerView = new FrameLayout(getApplicationContext());
      containerView.setId(containerViewId);
      containerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

      getBridge().getWebView().setBackgroundColor(Color.TRANSPARENT);
      ((ViewGroup)getBridge().getWebView().getParent()).addView(containerView);
      // to back
      getBridge().getWebView().getParent().bringChildToFront(getBridge().getWebView());

      FragmentManager fragmentManager = getSupportFragmentManager();
      FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
      fragmentTransaction.add(containerView.getId(), fragment);
      fragmentTransaction.commit();
    }

    //main logic or main code
    System.out.println("START NODE");
    if( !_startedNodeAlready ) {
      System.out.println("START NODE BECAUSE NOT STARTED YET");
      _startedNodeAlready=true;
      new Thread(new Runnable() {
        @Override
        public void run() {
          //The path where we expect the node project to be at runtime.
          System.out.println("START NODE RUN LOOP");
          String nodeDir=getApplicationContext().getFilesDir().getAbsolutePath()+"/nodejs-project";
          if (wasAPKUpdated()) {
            System.out.println("START NODE APK UPDATED");
            //Recursively delete any existing nodejs-project.
            File nodeDirReference=new File(nodeDir);
            if (nodeDirReference.exists()) {
              deleteFolderRecursively(new File(nodeDir));
            }

            // create dest folder
            new File(nodeDir).mkdirs();

            System.out.println("START NODE COPY ASSET FOLDER");
            //Copy the node project from assets into the application's data path.
            copyAndUnzip("nodejs-project.zip", nodeDir);
            // slow way is to copy the actual files
            // copyAssetFolder(getApplicationContext().getAssets(), "nodejs-project", nodeDir);


            System.out.println("START NODE COPY ASSET FOLDER FINISH");
            saveLastUpdateTime();
          }

          System.out.println("START NODE , REALLY");

          try {
            Os.setenv("PORT", "8080", true);
            Os.setenv("NODE_ENV", "production", true);
          } catch (ErrnoException e) {
            e.printStackTrace();
          }


          startNodeWithArguments(new String[]{"node",
                  nodeDir+"/server.js"
          });






        }
      }).start();
    }

  }

  /**
   * A native method that is implemented by the 'nodejsmobile' native library,
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
    byte[] buffer = new byte[8192];
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

  private void copyAndUnzip(String zipFilename, String toPath) {
    AssetManager assetManager = getApplicationContext().getAssets();

    try {
      System.out.println("Start unzip");
      InputStream inputStream = assetManager.open(zipFilename);
      ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

      ZipEntry zipEntry;
      byte[] buffer = new byte[8192];

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        String fileOrDirectory = zipEntry.getName();

        //System.out.println(fileOrDirectory);

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("file");
        builder.appendPath(toPath);
        builder.appendPath(fileOrDirectory);
        String fullToPath = builder.build().getPath();

        if (zipEntry.isDirectory()) {
          File directory = new File(fullToPath);
          directory.mkdirs();
          continue;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(fullToPath);
        int count;
        while ((count = zipInputStream.read(buffer)) != -1) {
          fileOutputStream.write(buffer, 0, count);
        }
        fileOutputStream.close();
        zipInputStream.closeEntry();
      }

      zipInputStream.close();

    } catch (IOException e) {
      Log.e("CopyAndUnzip", e.getLocalizedMessage());
    }
  }

  private boolean checkPermission() {
    if (getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
      // Permission is not granted
      return false;
    }
    return true;
  }

  private void requestPermission() {

    this.requestPermissions(
            new String[]{Manifest.permission.CAMERA},
            PERMISSION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQUEST_CODE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
          mainlogic();
          // main logic
        } else {
          Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
              showMessageOKCancel("You need to allow access permissions",
                      new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermission();
                          }
                        }
                      });
            }
          }
        }
        break;
    }
  }

  private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
    new AlertDialog.Builder(getApplicationContext())
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show();
  }

  // Taken from this gist https://gist.github.com/wangsy/417e39ea24250958977c9538579c1d3e
  public void attachDownloadManagerToWebView(WebView webview) {
    webview.setDownloadListener(new DownloadListener() {

      @Override
      public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

        try {
          DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
          request.setMimeType(mimeType);
          request.addRequestHeader("User-Agent", userAgent);
          String fileName = contentDisposition.replace("inline; filename=", "");
          fileName = fileName.replaceAll(".+UTF-8''", "");
          fileName = fileName.replaceAll("\"", "");
          fileName = URLDecoder.decode(fileName, "UTF-8");
          // clean name
          fileName = fileName.replace("attachment; filename=","");
          request.setDescription("Downloading "+ fileName +" in the download folder");
          request.setTitle(fileName);
          request.allowScanningByMediaScanner();
          request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
          request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
          DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
          dm.enqueue(request);
          Toast.makeText(getApplicationContext(), "Downloading "+ fileName +" in the download folder", Toast.LENGTH_LONG).show();
        } catch (Exception e) {

          if (ContextCompat.checkSelfPermission(MainActivity.this,
                  android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                  != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
              Toast.makeText(getBaseContext(), "OpenDataCam needs your permission to be able to write file to the download folder", Toast.LENGTH_LONG).show();
              ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                      110);
            } else {
              Toast.makeText(getBaseContext(), "OpenDataCam needs your permission to be able to write file to the download folder", Toast.LENGTH_LONG).show();
              ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                      110);
            }
          }
        }
      }
    });
  }

}
