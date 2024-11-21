package com.neurotec.tutorials.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public abstract class BaseActivity extends Activity {

    protected class ImageData {
        private Bitmap image;
        private String name;

        public ImageData(Bitmap image, String name) {
            this.image = image;
            this.name = name;
        }

        public Bitmap getImage() {
            return image;
        }

        public String getName() {
            return name;
        }
    }

    protected interface OnSelectionListener {
        void onFileSelected(Uri path);
    }

    // ===========================================================
    // Private fields
    // ===========================================================

    private OnSelectionListener mFileSelectionListener = null;

    // ===========================================================
    // Protected fields
    // ===========================================================

    protected static final int REQUEST_TO_SAVE_FILE_ID = 9999;

    // ===========================================================
    // Protected methods
    // ===========================================================

    private static String getExtension(String fileName) {
        String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
        if (tokens.length > 1) {
            return tokens[1];
        } else {
            throw new IllegalArgumentException("Filename does not contain extension");
        }
    }

    protected static void saveDataToUri(Context context, Uri uri, byte[] data) throws IOException {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w");
             FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
            fos.write(data);
        }
    }

    protected void requestToSaveFile(String filename, OnSelectionListener listener) {
        this.mFileSelectionListener = listener;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        StringBuilder mime = new StringBuilder();
        mime.append("application/");
        mime.append(getExtension(filename));
        intent.setType(mime.toString());
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, REQUEST_TO_SAVE_FILE_ID);
    }

    protected void showToast(final String message) {
        runOnUiThread(() -> ToastManager.show(BaseActivity.this, message));
    }

    protected void showDialog(final String message) {
        runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(BaseActivity.this).create();
            dialog.setTitle("Error");
            dialog.setMessage(message);
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            dialog.show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_TO_SAVE_FILE_ID) {
            if (resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    mFileSelectionListener.onFileSelected(resultData.getData());
                    mFileSelectionListener = null;
                }
            }
        }
    }
}
