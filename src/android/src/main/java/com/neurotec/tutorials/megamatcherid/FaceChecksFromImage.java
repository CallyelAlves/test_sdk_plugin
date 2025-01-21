package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;

import android.util.Base64;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NIcaoWarnings;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.cordova.CallbackContext;

public final class FaceChecksFromImage extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int IMAGE_REQUEST_CODE = 3;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;
    private RadioButton mIcaoTrueButton;
    private RadioButton mLivenessTrueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(2131296285);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        // Setting up activity components
        mIcaoTrueButton = findViewById(2131165296);
        mLivenessTrueButton = findViewById(2131165319);
        Button checkFromImageButton = findViewById(2131165243);
        checkFromImageButton.setOnClickListener(v -> openFile(IMAGE_REQUEST_CODE, "image/*"));
    }

    // Activity components results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_REQUEST_CODE) {
                // checksFromImage(uriToBytes(data.getData()));
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

    // Helper function to open image.
    public void openFile(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
    }

    // Helper function to get bytes from uri.
    public byte[] uriToBytes(Uri uri) {
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

    // Main activity function.
    public void checksFromImage(byte[] imageBytes, CallbackContext callbackContext) {
        new Thread(() -> {
            try {
                callbackContext.success(Base64.encodeToString(imageBytes, Base64.DEFAULT));
                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                mMMID.setCheckIcaoCompliance(mIcaoTrueButton.isChecked());
                mMMID.setDetectLivenessFromImage(mLivenessTrueButton.isChecked());
                
                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

                // Start checks from image.
                byte[] registrationKey = mMMID.startChecksFromImage(imageBytes);

                // After checks are finished finished, the registration key must be sent to the licensing server to acquire the server key.
                byte[] serverKey = mOperationApi.validate(registrationKey);

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishOperation(serverKey);

                // Check if image checks succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    String failedMsg;
                    if (result.getIcaoWarnings().size() > 0) {
                        failedMsg = "Failed. ICAO Warnings: ";
                        for (NIcaoWarnings warning: result.getIcaoWarnings()) {
                            failedMsg += warning.name() + " ";
                        }
                    } else {
                        failedMsg = "Checking failed: " + result.getStatus();
                    }
                    showToast(failedMsg);
                    return;
                }

                // callbackContext.success(String.format("Success with %s quality, Estimated age: %d", result.getQuality(), result.getAge()));

                // Get quality and age from result.
                showToast(String.format("Success with %s quality, Estimated age: %d", result.getQuality(), result.getAge()));
                /*
                The final result also includes:
                    - Image/token image
                    - JPEG2000 image/token image.
                Also, as NOperationResult extends NCapturePreview, it can be used to get all of the preview properties.
                */
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed checksFromImage function: " + e.getMessage());
            }
        }).start();
    }

    // Helper function to save template.
    private void saveTemplate(byte[] template) {
        showToast("Select where to save the template");
        requestToSaveFile("MMIDTemplate.dat", path -> {
            try {
                saveDataToUri(FaceChecksFromImage.this, path, template);
                showToast("MMIDTemplate saved to " + path.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed saveDataToUri in requestToSaveFile function: " + e.getMessage());
            }
        });
    }
}
