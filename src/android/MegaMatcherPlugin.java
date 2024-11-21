package com.example.megamatcherplugin;

import com.neurotec.biometrics.NFace;
import com.neurotec.biometrics.NBiometricClient;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

public class MegaMatcherPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("recognizeFace")) {
            this.recognizeFace(callbackContext);
            return true;
        }
        return false;
    }

    private void recognizeFace(CallbackContext callbackContext) {
        NBiometricClient client = new NBiometricClient();
        NFace face = new NFace();

        callbackContext.success("Face recognized successfully");
    }
}
