package com.neurotec.tutorials.megamatcherid;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;

public class MegaMatcherIdTutorials extends ListActivity {

    // ===========================================================
    // Private static fields
    // ===========================================================

    private static final Comparator<Map<String, Object>> DISPLAY_NAME_COMPARATOR = new Comparator<Map<String, Object>>() {
        private final Collator collator = Collator.getInstance();

        @Override
        public int compare(Map<String, Object> map1, Map<String, Object> map2) {
            return collator.compare(map1.get(KEY_TITLE), map2.get(KEY_TITLE));
        }
    };
    private static final String KEY_TITLE = "title";
    private static final String KEY_INTENT = "intent";

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private static final String WARNING_PROCEED_WITH_NOT_GRANTED_PERMISSIONS = "Do you wish to proceed without granting all permissions?";
    private static final String WARNING_NOT_ALL_GRANTED = "Some permissions are not granted.";
    private static final String MESSAGE_ALL_PERMISSIONS_GRANTED = "All permissions granted";

    private static final String TAG = MegaMatcherIdTutorials.class.getSimpleName();

    private static final String FLASH_MODE = "flash_mode";
    private static final String FLASH_MODE_FLASH_ON = "on";
    private static final String FLASH_MODE_FLASH_TORCH = "torch";
    private static final String FLASH_MODE_FLASH_OFF = "off";

    // ===========================================================
    // Public static fields
    // ===========================================================

    public static final String CATEGORY_NEUROTEC_TUTORIAL = MegaMatcherIdTutorials.class.getPackage().getName() + ".CATEGORY_NEUROTEC_TUTORIAL";

    // ===========================================================
    // Private methods
    // ===========================================================

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> myData = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(CATEGORY_NEUROTEC_TUTORIAL);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        if (null == list) {
            return myData;
        }

        int len = list.size();

        for (int i = 0; i < len; i++) {
            ResolveInfo info = list.get(i);
            CharSequence labelSeq = info.loadLabel(pm);
            String label;
            if (labelSeq == null) {
                label = info.activityInfo.name;
            } else {
                label = labelSeq.toString();
            }
            addItem(myData, label, activityIntent(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
        }

        Collections.sort(myData, DISPLAY_NAME_COMPARATOR);

        return myData;
    }

    private Intent activityIntent(String pkg, String componentName) {
        Intent result = new Intent();
        result.setClassName(pkg, componentName);
        return result;
    }

    private void addItem(List<Map<String, Object>> data, String name, Intent intent) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(KEY_TITLE, name);
        temp.put(KEY_INTENT, intent);
        data.add(temp);
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private static List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.RECORD_AUDIO);

        if (android.os.Build.VERSION.SDK_INT < 23) {
            permissions.add(Manifest.permission.WRITE_SETTINGS);
        }
        return permissions;
    }

    private String[] getNotGrantedPermissions() {
        List<String> neededPermissions = new ArrayList<>();

        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        return neededPermissions.toArray(new String[neededPermissions.size()]);
    }

    private void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
    }

    // ===========================================================
    // Activity events
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MegaMatcherIdClient.setContext(this);
        setListAdapter(new SimpleAdapter(this, getData(), android.R.layout.simple_list_item_1, new String[]{KEY_TITLE}, new int[]{android.R.id.text1}));
        getListView().setTextFilterEnabled(true);
        String[] neededPermissions = getNotGrantedPermissions();
        if (neededPermissions.length != 0) {
            requestPermissions(neededPermissions);
        }
        // For better fingerprint image quality.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(FLASH_MODE, FLASH_MODE_FLASH_ON).apply();
    }

    private boolean ifAllPermissionsGranted(int[] results) {
        boolean finalResult = true;
        for (int permissionResult : results) {
            finalResult = (permissionResult == PackageManager.PERMISSION_GRANTED);
            if (!finalResult) break;
        }
        return finalResult;
    }

    public void onRequestPermissionsResult(int requestCode, final String[] permissions, final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    // Check if all permissions granted
                    if (!ifAllPermissionsGranted(grantResults)) {
                        showDialogOK(WARNING_PROCEED_WITH_NOT_GRANTED_PERMISSIONS,
                                (dialog, which) -> {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            Log.w(TAG, WARNING_NOT_ALL_GRANTED);
                                            for(int i = 0; i < permissions.length;i++) {
                                                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                                    Log.w(TAG, permissions[i] + ": PERMISSION_DENIED");
                                                }
                                            }
                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            requestPermissions(permissions);
                                            break;
                                        default:
                                            throw new AssertionError("Unrecognised permission dialog parameter value");
                                    }
                                });
                    } else {
                        Log.i(TAG, MESSAGE_ALL_PERMISSIONS_GRANTED);
                    }
                }
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, Object> map = (Map<String, Object>) l.getItemAtPosition(position);
        Intent intent = (Intent) map.get(KEY_INTENT);
        startActivity(intent);
    }
}
