package com.neurotec.tutorials.megamatcherid;

import com.neurotec.tutorials.megamatcherid.FaceChecks;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

public class MegaMatcherPlugin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        System.out.println("recognizeFace");
        callbackContext.success("Face checks initiated successfully");
        if (action.equals("recognizeFace")) {
            this.checkPermissions();
            this.startFaceChecks(callbackContext);
            return true;
        }
        return false;
    }

    private void startFaceChecks(CallbackContext callbackContext) {
        try {
            // this.configureModelPath();
            Log.d("MegaMatcherPlugin", "Initializing FaceChecks...");
            FaceChecks faceChecks = new FaceChecks();
            faceChecks.checks();

            Log.d("MegaMatcherPlugin", "FaceChecks initialized successfully");
            callbackContext.success("Face checks initiated successfully");
        } catch (Exception e) {
            Log.e("MegaMatcherPlugin", "Error initiating face checks: " + e.getMessage(), e);
            callbackContext.error("Error initiating face checks: " + e.getMessage());
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(cordova.getActivity(),
                        new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    private void configureModelPath() {
        try {
            // Configura o caminho para os arquivos de modelo nos assets
            String modelPath = "file:///android_asset/MegaMatcherIdFaces.ndf";
            mMMID.setModelPath(modelPath);
            Log.d("MegaMatcherPlugin", "Model path set to: " + modelPath);
        } catch (Exception e) {
            Log.e("MegaMatcherPlugin", "Failed to configure model path", e);
        }
    }

}
