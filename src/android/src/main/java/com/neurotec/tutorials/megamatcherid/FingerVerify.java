package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NFinger;
import com.neurotec.mega.matcher.id.client.NImageSource;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FingerVerify extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int TEMPLATE_REQUEST_CODE = 3;

    private Button mCaptureButton;
    private Button mSelectTemplateButton;

    private MegaMatcherIdClient mMMID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finger_verify);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(v -> mMMID.force());
        mCaptureButton.setEnabled(false);

        mSelectTemplateButton = findViewById(R.id.open_template_button);
        mSelectTemplateButton.setOnClickListener(v -> selectTemplate(TEMPLATE_REQUEST_CODE, "*/*"));
    }

    // Activity components results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TEMPLATE_REQUEST_CODE) {
                verify(uriToBytes(data.getData()));
            }
        }
    }

    // Clean up after completing.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mMMID.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper function to get bytes from uri.
    private byte[] uriToBytes(Uri uri) {
        byte[] buf = new byte[0];

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int bytesRead;
            byte[] tempBuffer = new byte[READ_BUFFER_SIZE];
            while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
                buffer.write(tempBuffer, 0, bytesRead);
            }
            buf = buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Failed uriToBytes function: " + e.getMessage());
        }

        return buf;
    }

    // Helper function to select template.
    private void selectTemplate(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
    }

    // Main activity function.
    private void verify(byte[] data) {
        new Thread(() -> {
            try {
                // Get MMID template data.
                byte[] template = data;

                // Find all available cameras.
                String[] cameras = mMMID.getAvailableCameraNames();
                if (cameras.length == 0) {
                    System.out.println("No camera detected");
                }

                // Use the first camera that's available.
                String camera = Arrays.asList(mMMID.getAvailableCameraNames()).get(0);
                mMMID.setCurrentCamera(camera);
                showToast("Capturing with: " + camera);

                // Sets the required finger count, which is reset after the capture. Default is 4.
                mMMID.setRequiredFingersCount(4);

                // Sets whether the finger import image is captured by a scanner or by a camera. Default is true.
                mMMID.setFingerImageSource(NImageSource.CAMERA);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FINGERS_LEFT_HAND);

                // Start verify.
                NOperationResult result = mMMID.verify(template);

                // Check if checks succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                // Get quality from result.
                showToast(String.format("Success with %s quality", result.getQuality()));

            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mSelectTemplateButton.setEnabled(true);
                    mCaptureButton.setEnabled(false);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mSelectTemplateButton.setEnabled(false);
            mCaptureButton.setEnabled(true);
        });
    }
}
