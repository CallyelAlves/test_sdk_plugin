package com.neurotec.tutorials.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public final class ToastManager {

    // ===========================================================
    // Private static fields
    // ===========================================================

    private static Toast toastMessage;
    private static final int DURATION = 4000;
    private static final Map<Object, Long> sLastShown = new HashMap<>();

    // ===========================================================
    // private constructor
    // ===========================================================

    private ToastManager() {
    }

    // ===========================================================
    // Private static methods
    // ===========================================================

    private static boolean isRecent(Object obj) {
        Long last = sLastShown.get(obj);
        if (last == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (last + DURATION < now) {
            return false;
        }
        return true;
    }

    // ===========================================================
    // Public static methods
    // ===========================================================

    public static synchronized void show(Context context, String msg) {
        if (context == null) throw new NullPointerException("context");
        if (isRecent(msg)) {
            return;
        }

        if (toastMessage != null) {
            toastMessage.cancel();
        }
        toastMessage = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toastMessage.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL, 10, 10);
        toastMessage.show();
        sLastShown.put(msg, System.currentTimeMillis());
    }

}
